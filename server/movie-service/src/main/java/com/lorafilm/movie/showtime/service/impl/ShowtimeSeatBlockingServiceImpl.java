package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.seat.domain.entity.Seat;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeBlockedSeatsRequest;
import com.lorafilm.movie.showtime.dto.response.ShowtimeSeatControlResponse;
import com.lorafilm.movie.showtime.integration.BookingSeatAvailabilityClient;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.service.ShowtimeSeatBlockingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShowtimeSeatBlockingServiceImpl implements ShowtimeSeatBlockingService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeBlockedSeatRepository blockedSeatRepository;
    private final BookingSeatAvailabilityClient bookingAvailabilityClient;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ShowtimeSeatBlockingServiceImpl(
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            ShowtimeBlockedSeatRepository blockedSeatRepository,
            BookingSeatAvailabilityClient bookingAvailabilityClient,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.blockedSeatRepository = blockedSeatRepository;
        this.bookingAvailabilityClient = bookingAvailabilityClient;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeSeatControlResponse getSeatControl(String showtimePublicId) {
        Showtime showtime = requireShowtime(showtimePublicId, false);
        return buildResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeSeatControlResponse blockSeats(
            String showtimePublicId,
            UpdateShowtimeBlockedSeatsRequest request) {
        Showtime showtime = requireShowtime(showtimePublicId, true);
        requireEditable(showtime);
        String reason = normalizeRequiredReason(request.reason());
        List<Seat> selectedSeats = resolveSelectedSeats(showtime, request.seatPublicIds());
        List<Long> seatIds = selectedSeats.stream().map(Seat::getId).toList();

        List<ShowtimeBlockedSeat> existingRows = blockedSeatRepository.findForUpdate(showtime.getId(), seatIds);
        Set<Long> alreadyBlocked = existingRows.stream()
                .filter(row -> row.getStatus() == ActionStatus.ACTIVE)
                .map(row -> row.getSeat().getId())
                .collect(Collectors.toSet());
        List<Seat> newlyBlockedSeats = selectedSeats.stream()
                .filter(seat -> !alreadyBlocked.contains(seat.getId()))
                .toList();

        verifySeatsAreAvailable(showtime, newlyBlockedSeats);

        Map<Long, List<ShowtimeBlockedSeat>> rowsBySeat = existingRows.stream()
                .collect(Collectors.groupingBy(row -> row.getSeat().getId(), LinkedHashMap::new, Collectors.toList()));
        Long actorId = currentUserProvider.getCurrentUserId();
        List<ShowtimeBlockedSeat> changedRows = new ArrayList<>();
        for (Seat seat : selectedSeats) {
            List<ShowtimeBlockedSeat> seatRows = rowsBySeat.getOrDefault(seat.getId(), List.of());
            ShowtimeBlockedSeat row = seatRows.stream()
                    .filter(value -> value.getStatus() == ActionStatus.ACTIVE)
                    .findFirst()
                    .orElseGet(() -> seatRows.stream().findFirst().orElseGet(ShowtimeBlockedSeat::new));
            if (row.getId() == null) {
                row.setShowtime(showtime);
                row.setSeat(seat);
                row.setCreatedBy(actorId);
            }
            row.setStatus(ActionStatus.ACTIVE);
            row.setReason(reason);
            row.setUpdatedBy(actorId);
            changedRows.add(row);
        }
        blockedSeatRepository.saveAll(changedRows);
        blockedSeatRepository.flush();
        return buildResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeSeatControlResponse releaseSeats(
            String showtimePublicId,
            UpdateShowtimeBlockedSeatsRequest request) {
        Showtime showtime = requireShowtime(showtimePublicId, true);
        requireEditable(showtime);
        List<Seat> selectedSeats = resolveSelectedSeats(showtime, request.seatPublicIds());
        List<Long> seatIds = selectedSeats.stream().map(Seat::getId).toList();
        List<ShowtimeBlockedSeat> rows = blockedSeatRepository.findForUpdate(showtime.getId(), seatIds);
        Long actorId = currentUserProvider.getCurrentUserId();
        String releaseReason = request.reason() == null || request.reason().isBlank()
                ? "Đã mở lại ghế cho khách đặt vé"
                : request.reason().trim();
        rows.stream()
                .filter(row -> row.getStatus() == ActionStatus.ACTIVE)
                .forEach(row -> {
                    row.setStatus(ActionStatus.CANCELLED);
                    row.setReason(releaseReason);
                    row.setUpdatedBy(actorId);
                });
        blockedSeatRepository.flush();
        return buildResponse(showtime);
    }

    private Showtime requireShowtime(String publicId, boolean forUpdate) {
        return (forUpdate
                ? showtimeRepository.findByPublicIdForUpdate(publicId)
                : showtimeRepository.findByPublicIdAndDeletedAtIsNull(publicId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND));
    }

    private void requireEditable(Showtime showtime) {
        if (!isEditable(showtime)) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_SEAT_CONTROL_NOT_EDITABLE,
                    editabilityMessage(showtime));
        }
    }

    private boolean isEditable(Showtime showtime) {
        if (!showtime.getStartTime().isAfter(Instant.now(clock))) return false;
        return showtime.getStatus() == ShowtimeStatus.DRAFT
                || showtime.getStatus() == ShowtimeStatus.OPEN_FOR_BOOKING;
    }

    private String editabilityMessage(Showtime showtime) {
        if (!showtime.getStartTime().isAfter(Instant.now(clock))) {
            return "Suất chiếu đã bắt đầu hoặc đã qua giờ nên chỉ có thể xem sơ đồ ghế.";
        }
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            return "Suất chiếu đã hủy nên không thể thay đổi ghế.";
        }
        if (showtime.getStatus() == ShowtimeStatus.FINISHED) {
            return "Suất chiếu đã kết thúc nên không thể thay đổi ghế.";
        }
        return "Chỉ có thể khóa ghế khi suất đang soạn hoặc đang mở bán.";
    }

    private String normalizeRequiredReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng chọn hoặc nhập lý do khóa ghế.");
        }
        return reason.trim();
    }

    private List<Seat> resolveSelectedSeats(Showtime showtime, Collection<String> requestedPublicIds) {
        LinkedHashSet<String> normalizedIds = requestedPublicIds == null
                ? new LinkedHashSet<>()
                : requestedPublicIds.stream()
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vui lòng chọn ít nhất một ghế.");
        }

        List<Seat> auditoriumSeats = seatRepository.findAdminLayoutByAuditoriumId(showtime.getAuditorium().getId());
        Map<String, Seat> byPublicId = auditoriumSeats.stream()
                .collect(Collectors.toMap(Seat::getPublicId, Function.identity()));
        List<String> missingIds = normalizedIds.stream().filter(id -> !byPublicId.containsKey(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_FOUND, "Có ghế không thuộc phòng chiếu của suất này.", missingIds);
        }

        LinkedHashSet<Seat> selected = normalizedIds.stream()
                .map(byPublicId::get)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> selectedPairGroups = selected.stream()
                .map(Seat::getPairGroup)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        if (!selectedPairGroups.isEmpty()) {
            auditoriumSeats.stream()
                    .filter(seat -> seat.getPairGroup() != null && selectedPairGroups.contains(seat.getPairGroup()))
                    .forEach(selected::add);
        }
        List<Seat> result = List.copyOf(selected);
        List<String> inactiveCodes = result.stream()
                .filter(seat -> seat.getStatus() != SeatStatus.ACTIVE)
                .map(Seat::getSeatCode)
                .toList();
        if (!inactiveCodes.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.SEAT_INACTIVE,
                    "Ghế đang bảo trì hoặc đã ngưng hoạt động không cần khóa riêng cho suất chiếu.",
                    inactiveCodes);
        }
        return result;
    }

    private void verifySeatsAreAvailable(Showtime showtime, List<Seat> seats) {
        if (showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING || seats.isEmpty()) return;
        BookingSeatAvailabilityClient.AvailabilityResult availability = bookingAvailabilityClient.check(
                showtime.getId(), seats.stream().map(Seat::getId).toList());
        if (!availability.verified()) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_SEAT_AVAILABILITY_UNAVAILABLE,
                    "Chưa thể kiểm tra ghế khách đang giữ hoặc đã mua. Vui lòng thử lại để tránh khóa nhầm ghế của khách.");
        }
        if (!availability.unavailableSeatIds().isEmpty()) {
            Set<Long> occupiedIds = Set.copyOf(availability.unavailableSeatIds());
            List<String> occupiedCodes = seats.stream()
                    .filter(seat -> occupiedIds.contains(seat.getId()))
                    .map(Seat::getSeatCode)
                    .toList();
            throw new BusinessException(
                    ErrorCode.SHOWTIME_SEAT_ALREADY_OCCUPIED,
                    "Không thể khóa ghế đang được khách giữ hoặc đã mua.",
                    occupiedCodes);
        }
    }

    private ShowtimeSeatControlResponse buildResponse(Showtime showtime) {
        List<Seat> seats = seatRepository.findAdminLayoutByAuditoriumId(showtime.getAuditorium().getId());
        Map<Long, ShowtimeBlockedSeat> activeBlocks = blockedSeatRepository
                .findByShowtimeIdAndStatus(showtime.getId(), ActionStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        row -> row.getSeat().getId(),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        List<ShowtimeSeatControlResponse.SeatItem> seatItems = seats.stream()
                .map(seat -> {
                    ShowtimeBlockedSeat block = activeBlocks.get(seat.getId());
                    return new ShowtimeSeatControlResponse.SeatItem(
                            seat.getPublicId(),
                            seat.getSeatCode(),
                            seat.getRowLabel(),
                            seat.getSeatNumber(),
                            seat.getPositionRow(),
                            seat.getPositionColumn(),
                            seat.getSeatType().getCode().name(),
                            seat.getSeatType().getName(),
                            seat.getPairGroup(),
                            seat.getStatus().name(),
                            block != null,
                            block == null ? null : block.getReason(),
                            block == null ? null : block.getUpdatedAt(),
                            block == null ? null : block.getUpdatedBy());
                })
                .toList();
        return new ShowtimeSeatControlResponse(
                showtime.getPublicId(),
                showtime.getStatus(),
                showtime.getMovie().getTitle(),
                showtime.getCinema().getPublicId(),
                showtime.getCinema().getName(),
                showtime.getCinema().getTimezone(),
                showtime.getAuditorium().getPublicId(),
                showtime.getAuditorium().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                isEditable(showtime),
                isEditable(showtime) ? null : editabilityMessage(showtime),
                activeBlocks.size(),
                seatItems);
    }
}
