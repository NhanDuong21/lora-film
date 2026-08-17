package com.lorafilm.movie.auditorium.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.domain.enums.MaintenanceType;
import com.lorafilm.movie.auditorium.dto.CreateMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.ExtendMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.dto.EmergencyMaintenanceSummaryResponse;
import com.lorafilm.movie.auditorium.dto.EmergencyPaidBookingHandoffResponse;
import com.lorafilm.movie.auditorium.dto.MaintenanceImpactResponse;
import com.lorafilm.movie.auditorium.dto.MaintenanceWindowResponse;
import com.lorafilm.movie.auditorium.dto.ResolveMaintenanceWindowRequest;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceImpactService;
import com.lorafilm.movie.auditorium.service.AuditoriumMaintenanceService;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.integration.BookingEmergencyClosureClient;
import com.lorafilm.movie.showtime.integration.PaymentEmergencyStopClient;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuditoriumMaintenanceServiceImpl implements AuditoriumMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriumMaintenanceServiceImpl.class);

    private final AuditoriumMaintenanceWindowRepository maintenanceRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final AuditoriumMaintenanceImpactService impactService;
    private final ShowtimeStatusTransitionService showtimeTransitionService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;
    private final BookingEmergencyClosureClient bookingEmergencyClosureClient;
    private final PaymentEmergencyStopClient paymentEmergencyStopClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditoriumMaintenanceServiceImpl(
            AuditoriumMaintenanceWindowRepository maintenanceRepository,
            AuditoriumRepository auditoriumRepository,
            AuditoriumMaintenanceImpactService impactService,
            ShowtimeStatusTransitionService showtimeTransitionService,
            CurrentUserProvider currentUserProvider,
            Clock clock,
            BookingEmergencyClosureClient bookingEmergencyClosureClient,
            PaymentEmergencyStopClient paymentEmergencyStopClient,
            ObjectMapper objectMapper) {
        this.maintenanceRepository = maintenanceRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.impactService = impactService;
        this.showtimeTransitionService = showtimeTransitionService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.bookingEmergencyClosureClient = bookingEmergencyClosureClient;
        this.paymentEmergencyStopClient = paymentEmergencyStopClient;
        this.objectMapper = objectMapper;
    }

    /** Backwards-compatible constructor for focused unit tests. */
    public AuditoriumMaintenanceServiceImpl(
            AuditoriumMaintenanceWindowRepository maintenanceRepository,
            AuditoriumRepository auditoriumRepository,
            AuditoriumMaintenanceImpactService impactService,
            ShowtimeStatusTransitionService showtimeTransitionService,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this(maintenanceRepository, auditoriumRepository, impactService,
                showtimeTransitionService, currentUserProvider, clock,
                null, null, new ObjectMapper().findAndRegisterModules());
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse createWindow(
            String auditoriumPublicId,
            CreateMaintenanceWindowRequest request) {
        Auditorium auditorium = auditoriumRepository
                .findByPublicIdAndDeletedAtIsNullForUpdate(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        Instant now = Instant.now(clock);
        MaintenanceType maintenanceType = request.maintenanceType() == null
                ? MaintenanceType.PLANNED
                : request.maintenanceType();
        Instant startTime = maintenanceType == MaintenanceType.EMERGENCY
                ? now
                : request.startTime();
        validateCreateTimeRange(startTime, request.endTime(), maintenanceType, now);

        Optional<AuditoriumMaintenanceWindow> overlap = maintenanceRepository.findFirstOverlap(
                auditorium.getId(), ActionStatus.ACTIVE, startTime, request.endTime());
        if (overlap.isPresent()) {
            throw overlapException(overlap.get());
        }

        CreateMaintenanceWindowRequest normalizedRequest = new CreateMaintenanceWindowRequest(
                startTime,
                request.endTime(),
                request.reason().trim(),
                maintenanceType);
        MaintenanceImpactResponse impact = impactService.preview(auditoriumPublicId, normalizedRequest);
        if (maintenanceType == MaintenanceType.PLANNED
                && (impact.affectedShowtimeCount() > 0 || !impact.bookingDataComplete())) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("affectedShowtimeCount", impact.affectedShowtimeCount());
            errorData.put("openForBookingCount", impact.openForBookingCount());
            errorData.put("occupiedSeatCount", impact.occupiedSeatCount());
            errorData.put("bookingDataComplete", impact.bookingDataComplete());
            throw new BusinessException(ErrorCode.PLANNED_MAINTENANCE_HAS_AFFECTED_SHOWTIMES, errorData);
        }

        Long currentUserId = currentUserProvider.getCurrentUserId();
        AuditoriumMaintenanceWindow window = new AuditoriumMaintenanceWindow();
        window.setAuditorium(auditorium);
        window.setStartTime(startTime);
        window.setEndTime(request.endTime());
        window.setReason(request.reason().trim());
        window.setMaintenanceType(maintenanceType);
        window.setStatus(ActionStatus.ACTIVE);
        window.setCreatedBy(currentUserId);
        window.setUpdatedBy(currentUserId);
        window = maintenanceRepository.saveAndFlush(window);

        if (maintenanceType == MaintenanceType.EMERGENCY) {
            EmergencyMaintenanceSummaryResponse summary =
                    closeAffectedShowtimesForEmergency(window, impact);
            window.setEmergencySummaryJson(writeEmergencySummary(summary));
            window = maintenanceRepository.saveAndFlush(window);
        }
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse cancelWindow(Long maintenanceWindowId) {
        AuditoriumMaintenanceWindow window = findWindowForUpdate(maintenanceWindowId);
        requireActive(window);
        if (!Instant.now(clock).isBefore(window.getStartTime())) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_CANCELLED_AFTER_START);
        }

        window.setStatus(ActionStatus.CANCELLED);
        window.setUpdatedBy(currentUserProvider.getCurrentUserId());
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse resolveWindow(
            Long maintenanceWindowId,
            ResolveMaintenanceWindowRequest request) {
        AuditoriumMaintenanceWindow window = findWindowForUpdate(maintenanceWindowId);
        requireActive(window);
        Instant now = Instant.now(clock);
        if (now.isBefore(window.getStartTime())) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_RESOLVED_BEFORE_START);
        }

        Long currentUserId = currentUserProvider.getCurrentUserId();
        window.setStatus(ActionStatus.RESOLVED);
        window.setActualEndTime(now);
        window.setResolvedBy(currentUserId);
        window.setResolutionNote(request.resolutionNote().trim());
        window.setUpdatedBy(currentUserId);
        return mapToResponse(window);
    }

    @Override
    @Transactional
    public MaintenanceWindowResponse extendWindow(
            Long maintenanceWindowId,
            ExtendMaintenanceWindowRequest request) {
        AuditoriumMaintenanceWindow window = findWindowForUpdate(maintenanceWindowId);
        requireActive(window);
        Instant now = Instant.now(clock);
        if (!request.endTime().isAfter(window.getEndTime()) || !request.endTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.MAINTENANCE_EXTENSION_MUST_INCREASE_END_TIME);
        }

        Optional<AuditoriumMaintenanceWindow> overlap = maintenanceRepository.findFirstOverlapExcluding(
                window.getAuditorium().getId(),
                window.getId(),
                ActionStatus.ACTIVE,
                window.getStartTime(),
                request.endTime());
        if (overlap.isPresent()) {
            throw overlapException(overlap.get());
        }

        CreateMaintenanceWindowRequest impactRequest = new CreateMaintenanceWindowRequest(
                window.getEndTime(),
                request.endTime(),
                window.getReason(),
                window.getMaintenanceType());
        MaintenanceImpactResponse impact = impactService.preview(
                window.getAuditorium().getPublicId(), impactRequest);
        if (window.getMaintenanceType() == MaintenanceType.PLANNED
                && (impact.affectedShowtimeCount() > 0 || !impact.bookingDataComplete())) {
            throw new BusinessException(ErrorCode.PLANNED_MAINTENANCE_HAS_AFFECTED_SHOWTIMES);
        }

        window.setEndTime(request.endTime());
        window.setExtensionNote(request.note().trim());
        window.setUpdatedBy(currentUserProvider.getCurrentUserId());
        if (window.getMaintenanceType() == MaintenanceType.EMERGENCY) {
            EmergencyMaintenanceSummaryResponse additional =
                    closeAffectedShowtimesForEmergency(window, impact);
            window.setEmergencySummaryJson(writeEmergencySummary(
                    mergeEmergencySummaries(readEmergencySummary(window), additional)));
        }
        return mapToResponse(window);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceWindowResponse> getMaintenanceWindows(String auditoriumPublicId) {
        Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(auditoriumPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));

        return maintenanceRepository.findByAuditoriumIdOrderByStartTimeDesc(auditorium.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateCreateTimeRange(
            Instant startTime,
            Instant endTime,
            MaintenanceType maintenanceType,
            Instant now) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_MAINTENANCE_TIME_RANGE);
        }
        if (maintenanceType == MaintenanceType.PLANNED && startTime.isBefore(now)) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_CANNOT_BE_CREATED_IN_PAST);
        }
    }

    private EmergencyMaintenanceSummaryResponse closeAffectedShowtimesForEmergency(
            AuditoriumMaintenanceWindow window,
            MaintenanceImpactResponse impact) {
        String reason = "Tự động đóng bán do sự cố phòng chiếu #" + window.getId()
                + ": " + window.getReason();
        List<EmergencyPaidBookingHandoffResponse> paidBookings = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int[] releasedSeatHolds = {0};
        int[] cancelledPendingBookings = {0};
        int[] stoppedPaymentAttempts = {0};
        boolean[] processingComplete = {true};

        var openShowtimes = impact.showtimes().stream()
                .filter(showtime -> showtime.status() == ShowtimeStatus.OPEN_FOR_BOOKING)
                .toList();
        openShowtimes.forEach(showtime -> {
            UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
            // The screening cannot take place, so this is a cancellation rather
            // than a normal sales close. The transition also creates the refund
            // outbox work for paid bookings.
            request.setStatus(ShowtimeStatus.CANCELLED);
            request.setReason(reason);
            showtimeTransitionService.transitionStatus(showtime.showtimePublicId(), request);

            if (bookingEmergencyClosureClient == null) {
                processingComplete[0] = false;
                warnings.add("Chưa thể đồng bộ giữ ghế và đơn chờ của suất "
                        + showtime.showtimePublicId());
                return;
            }
            try {
                BookingEmergencyClosureClient.EmergencyClosureResult bookingResult =
                        bookingEmergencyClosureClient.closeShowtime(
                                showtime.showtimePublicId(), reason);
                releasedSeatHolds[0] += bookingResult.releasedUnlinkedSeatCount();
                List<String> cancelledIds = bookingResult.cancelledPendingBookingPublicIds() == null
                        ? List.of()
                        : bookingResult.cancelledPendingBookingPublicIds();
                cancelledPendingBookings[0] += cancelledIds.size();
                addPaidHandoffs(paidBookings, showtime.showtimePublicId(), bookingResult.paidBookings());

                if (paymentEmergencyStopClient != null && !cancelledIds.isEmpty()) {
                    PaymentEmergencyStopClient.EmergencyPaymentStopResult paymentResult =
                            paymentEmergencyStopClient.stopPendingPayments(cancelledIds, reason);
                    stoppedPaymentAttempts[0] += paymentResult.stoppedPaymentAttemptCount();
                    addLateSuccessHandoffs(
                            paidBookings,
                            warnings,
                            showtime.showtimePublicId(),
                            bookingResult.cancelledPendingBookings(),
                            paymentResult.alreadySuccessfulBookingPublicIds());
                } else if (paymentEmergencyStopClient == null && !cancelledIds.isEmpty()) {
                    processingComplete[0] = false;
                    warnings.add("Chưa thể dừng các lần thanh toán của suất "
                            + showtime.showtimePublicId());
                }
            } catch (RuntimeException exception) {
                processingComplete[0] = false;
                warnings.add("Không thể hoàn tất đồng bộ đơn và thanh toán cho suất "
                        + showtime.showtimePublicId());
                log.warn("Emergency downstream handling failed for showtime {}",
                        showtime.showtimePublicId(), exception);
            }
        });
        return new EmergencyMaintenanceSummaryResponse(
                openShowtimes.size(),
                releasedSeatHolds[0],
                cancelledPendingBookings[0],
                stoppedPaymentAttempts[0],
                processingComplete[0],
                List.copyOf(paidBookings),
                List.copyOf(warnings));
    }

    private void addPaidHandoffs(
            List<EmergencyPaidBookingHandoffResponse> target,
            String showtimePublicId,
            List<BookingEmergencyClosureClient.PaidBooking> bookings) {
        if (bookings == null) {
            return;
        }
        bookings.forEach(booking -> target.add(
                toHandoff(showtimePublicId, booking, booking.bookingStatus())));
    }

    private void addLateSuccessHandoffs(
            List<EmergencyPaidBookingHandoffResponse> target,
            List<String> warnings,
            String showtimePublicId,
            List<BookingEmergencyClosureClient.PaidBooking> pendingSnapshots,
            List<String> successfulBookingIds) {
        if (successfulBookingIds == null || successfulBookingIds.isEmpty()) {
            return;
        }
        Map<String, BookingEmergencyClosureClient.PaidBooking> snapshots = new HashMap<>();
        if (pendingSnapshots != null) {
            pendingSnapshots.forEach(booking -> snapshots.put(booking.bookingPublicId(), booking));
        }
        successfulBookingIds.forEach(bookingPublicId -> {
            BookingEmergencyClosureClient.PaidBooking snapshot = snapshots.get(bookingPublicId);
            if (snapshot != null) {
                target.add(toHandoff(
                        showtimePublicId, snapshot, "PAYMENT_SUCCESS_DURING_CLOSURE"));
            }
        });
        warnings.add("Có " + successfulBookingIds.size()
                + " giao dịch vừa thành công khi đóng phòng; cần kiểm tra hoàn tiền ngay.");
    }

    private EmergencyPaidBookingHandoffResponse toHandoff(
            String showtimePublicId,
            BookingEmergencyClosureClient.PaidBooking booking,
            String bookingStatus) {
        return new EmergencyPaidBookingHandoffResponse(
                showtimePublicId,
                booking.bookingPublicId(),
                booking.bookingCode(),
                booking.userId(),
                bookingStatus,
                booking.finalAmount(),
                booking.currency(),
                booking.seatLabels() == null ? List.of() : booking.seatLabels());
    }

    private EmergencyMaintenanceSummaryResponse mergeEmergencySummaries(
            EmergencyMaintenanceSummaryResponse current,
            EmergencyMaintenanceSummaryResponse additional) {
        Map<String, EmergencyPaidBookingHandoffResponse> paidBookings = new LinkedHashMap<>();
        current.paidBookings().forEach(item -> paidBookings.put(item.bookingPublicId(), item));
        additional.paidBookings().forEach(item -> paidBookings.put(item.bookingPublicId(), item));
        List<String> warnings = new ArrayList<>(current.warnings());
        additional.warnings().stream()
                .filter(item -> !warnings.contains(item))
                .forEach(warnings::add);
        return new EmergencyMaintenanceSummaryResponse(
                current.closedShowtimeCount() + additional.closedShowtimeCount(),
                current.releasedSeatHoldCount() + additional.releasedSeatHoldCount(),
                current.cancelledPendingBookingCount() + additional.cancelledPendingBookingCount(),
                current.stoppedPaymentAttemptCount() + additional.stoppedPaymentAttemptCount(),
                current.processingComplete() && additional.processingComplete(),
                List.copyOf(paidBookings.values()),
                List.copyOf(warnings));
    }

    private String writeEmergencySummary(EmergencyMaintenanceSummaryResponse summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot store emergency maintenance handoff", exception);
        }
    }

    private EmergencyMaintenanceSummaryResponse readEmergencySummary(
            AuditoriumMaintenanceWindow window) {
        if (window.getEmergencySummaryJson() == null
                || window.getEmergencySummaryJson().isBlank()) {
            return EmergencyMaintenanceSummaryResponse.empty();
        }
        try {
            return objectMapper.readValue(
                    window.getEmergencySummaryJson(), EmergencyMaintenanceSummaryResponse.class);
        } catch (JsonProcessingException exception) {
            log.error("Cannot read emergency summary for maintenance window {}",
                    window.getId(), exception);
            return new EmergencyMaintenanceSummaryResponse(
                    0, 0, 0, 0, false, List.of(),
                    List.of("Không thể đọc dữ liệu bàn giao sự cố đã lưu"));
        }
    }

    private AuditoriumMaintenanceWindow findWindowForUpdate(Long maintenanceWindowId) {
        return maintenanceRepository.findByIdForUpdate(maintenanceWindowId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAINTENANCE_WINDOW_NOT_FOUND));
    }

    private void requireActive(AuditoriumMaintenanceWindow window) {
        if (window.getStatus() != ActionStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MAINTENANCE_WINDOW_NOT_ACTIVE);
        }
    }

    private BusinessException overlapException(AuditoriumMaintenanceWindow conflict) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("conflictingWindowId", conflict.getId());
        errorData.put("conflictingStartTime", conflict.getStartTime());
        errorData.put("conflictingEndTime", conflict.getEndTime());
        return new BusinessException(ErrorCode.MAINTENANCE_WINDOW_OVERLAPS, errorData);
    }

    private MaintenanceWindowResponse mapToResponse(AuditoriumMaintenanceWindow window) {
        return new MaintenanceWindowResponse(
                window.getId(),
                window.getAuditorium().getPublicId(),
                window.getStartTime(),
                window.getEndTime(),
                window.getReason(),
                window.getMaintenanceType(),
                window.getStatus(),
                window.getActualEndTime(),
                window.getResolvedBy(),
                window.getResolutionNote(),
                window.getExtensionNote(),
                readEmergencySummary(window),
                window.getCreatedAt(),
                window.getUpdatedAt(),
                window.getCreatedBy(),
                window.getUpdatedBy());
    }
}
