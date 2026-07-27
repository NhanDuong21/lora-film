package com.lorafilm.booking.booking.service.impl;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingDetailResponse;
import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.dto.BookingOperationalInfoDto;
import com.lorafilm.booking.booking.dto.BookingOperationsSummaryResponse;
import com.lorafilm.booking.booking.dto.BookingReservationAdminDto;
import com.lorafilm.booking.booking.dto.BookingSnapshotDto;
import com.lorafilm.booking.booking.dto.UpdateBookingStatusRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingAttentionFilter;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.mapper.BookingMapper;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSpecification;
import com.lorafilm.booking.booking.service.AdminBookingService;
import com.lorafilm.booking.booking.service.BookingSnapshotService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.BookingStatusTransitionService;
import com.lorafilm.booking.booking.service.BookingTicketService;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.response.PagedResponse;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminBookingServiceImpl implements AdminBookingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingStatusTransitionService statusTransitionService;
    private final BookingStatusHistoryService historyService;
    private final BookingAuditService auditService;
    private final BookingOperationLogService operationLogService;
    private final BookingOutboxService outboxService;
    private final BookingTicketService ticketService;
    private final BookingSnapshotService snapshotService;
    private final BookingMetricsManager bookingMetricsManager;
    private final SeatReservationService reservationService;
    private final BookingLifecycleService lifecycleService;
    private final SeatReservationRepository seatReservationRepository;
    private final BookingPaymentEventRepository paymentEventRepository;

    @Autowired
    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                  BookingMapper bookingMapper,
                                  BookingStatusTransitionService statusTransitionService,
                                  BookingStatusHistoryService historyService,
                                  BookingAuditService auditService,
                                  BookingOperationLogService operationLogService,
                                  BookingOutboxService outboxService,
                                  BookingTicketService ticketService,
                                  BookingSnapshotService snapshotService,
                                  BookingMetricsManager bookingMetricsManager,
                                  SeatReservationService reservationService,
                                  BookingLifecycleService lifecycleService,
                                  SeatReservationRepository seatReservationRepository,
                                  BookingPaymentEventRepository paymentEventRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.statusTransitionService = statusTransitionService;
        this.historyService = historyService;
        this.auditService = auditService;
        this.operationLogService = operationLogService;
        this.outboxService = outboxService;
        this.ticketService = ticketService;
        this.snapshotService = snapshotService;
        this.bookingMetricsManager = bookingMetricsManager;
        this.reservationService = reservationService;
        this.lifecycleService = lifecycleService;
        this.seatReservationRepository = seatReservationRepository;
        this.paymentEventRepository = paymentEventRepository;
    }

    /** Backwards-compatible constructor for existing unit callers. */
    public AdminBookingServiceImpl(BookingRepository bookingRepository,
                                  BookingMapper bookingMapper,
                                  BookingStatusTransitionService statusTransitionService,
                                  BookingStatusHistoryService historyService,
                                  BookingAuditService auditService,
                                  BookingOperationLogService operationLogService,
                                  BookingOutboxService outboxService,
                                  BookingTicketService ticketService,
                                  BookingSnapshotService snapshotService,
                                  BookingMetricsManager bookingMetricsManager) {
        this(bookingRepository, bookingMapper, statusTransitionService, historyService,
                auditService, operationLogService, outboxService, ticketService,
                snapshotService, bookingMetricsManager, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingAdminResponse> findBookings(BookingFilterRequest filter) {
        int page = filter != null ? filter.getPage() : 0;
        int size = filter != null && filter.getSize() > 0 ? filter.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookingPage = bookingRepository.findAll(BookingSpecification.filterBy(filter), pageable);

        Set<Long> attemptedBookingIds = findAttemptedBookingIds(bookingPage.getContent());
        List<BookingAdminResponse> content = bookingPage.getContent().stream()
                .map(booking -> toAdminSummary(
                        booking, attemptedBookingIds.contains(booking.getId())))
                .toList();

        return new PagedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BookingOperationsSummaryResponse getOperationsSummary() {
        Instant now = Instant.now();
        long total = bookingRepository.count(BookingSpecification.isNotDeleted());
        long pending = countByStatus(BookingStatus.PENDING_PAYMENT);
        long confirmed = countByStatus(BookingStatus.CONFIRMED);
        long completed = countByStatus(BookingStatus.COMPLETED);
        long cancelled = countByStatus(BookingStatus.CANCELLED);
        long expired = countByStatus(BookingStatus.EXPIRED);
        long refunded = countByStatus(BookingStatus.REFUNDED);
        long expiringSoon = countByAttention(BookingAttentionFilter.EXPIRING_SOON, now);
        long overdue = countByAttention(BookingAttentionFilter.OVERDUE, now);
        long paymentFailed = countByAttention(BookingAttentionFilter.PAYMENT_FAILED, now);
        long needsAttention = countByAttention(BookingAttentionFilter.NEEDS_ATTENTION, now);

        return new BookingOperationsSummaryResponse(
                total, pending, confirmed, completed, cancelled, expired, refunded,
                expiringSoon, overdue, paymentFailed, needsAttention);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(String publicId) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking public ID cannot be null or empty");
        }

        MDC.put("bookingId", publicId);
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(java.util.UUID.fromString(publicId)));

        BookingDetailResponse detailResponse = bookingMapper.toAdminDetailResponse(booking);
        Long dbBookingId = booking.getId();

        detailResponse.setSnapshot(snapshotService.findByBooking(dbBookingId));
        detailResponse.setTickets(ticketService.findByBooking(dbBookingId));
        detailResponse.setStatusHistories(historyService.findByBooking(dbBookingId));
        List<SeatReservation> reservations = seatReservationRepository == null
                ? Collections.emptyList()
                : seatReservationRepository.findAllByBookingId(dbBookingId);
        boolean paymentAttempted = paymentEventRepository != null
                && paymentEventRepository.existsByBookingId(dbBookingId);
        detailResponse.setReservations(reservations.stream()
                .map(this::toReservationAdminDto)
                .toList());
        detailResponse.setOperationalInfo(
                toOperationalInfo(booking, reservations, paymentAttempted, Instant.now()));

        return detailResponse;
    }

    private BookingAdminResponse toAdminSummary(Booking booking, boolean paymentAttempted) {
        BookingAdminResponse response = bookingMapper.toAdminResponse(booking);
        response.setPaymentAttempted(paymentAttempted);
        response.setAttentionCode(resolveAttentionCode(booking, Instant.now()));
        BookingSnapshotDto snapshot = snapshotService.findByBooking(booking.getId());
        if (snapshot == null) {
            return response;
        }
        response.setMovieTitle(snapshot.getMovieTitle());
        response.setMoviePosterUrl(snapshot.getMoviePoster());
        response.setCinemaName(snapshot.getCinemaName());
        response.setAuditoriumName(snapshot.getAuditoriumName());
        response.setShowtimeStart(snapshot.getShowtimeStart());
        response.setSeatCount(snapshot.getSeatCount());
        return response;
    }

    private Set<Long> findAttemptedBookingIds(List<Booking> bookings) {
        if (paymentEventRepository == null || bookings.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
        return new HashSet<>(paymentEventRepository.findBookingIdsWithEvents(bookingIds));
    }

    private long countByStatus(BookingStatus status) {
        return bookingRepository.count(
                BookingSpecification.isNotDeleted()
                        .and(BookingSpecification.hasStatus(status)));
    }

    private long countByAttention(BookingAttentionFilter attention, Instant now) {
        return bookingRepository.count(
                BookingSpecification.isNotDeleted()
                        .and(BookingSpecification.attention(attention, now)));
    }

    private BookingReservationAdminDto toReservationAdminDto(SeatReservation reservation) {
        return new BookingReservationAdminDto(
                reservation.getPublicId(),
                reservation.getSeatPublicId(),
                reservation.getSeatLabel(),
                reservation.getSeatType(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getExpiresAt(),
                reservation.getUpdatedAt(),
                reservation.getExpiredReason());
    }

    private BookingOperationalInfoDto toOperationalInfo(
            Booking booking,
            List<SeatReservation> reservations,
            boolean paymentAttempted,
            Instant now) {
        int held = countReservations(reservations, SeatReservationStatus.HELD);
        int booked = countReservations(reservations, SeatReservationStatus.BOOKED);
        int released = countReservations(reservations, SeatReservationStatus.RELEASED);
        int expired = countReservations(reservations, SeatReservationStatus.EXPIRED);
        String reservationState = resolveReservationState(
                reservations.size(), held, booked, released, expired);
        String reasonCode = booking.getCancelReasonCode();
        String reasonDetail = booking.getCancelReasonDetail();
        if (reasonDetail == null && booking.getBookingStatus() == BookingStatus.EXPIRED) {
            reasonDetail = reservations.stream()
                    .map(SeatReservation::getExpiredReason)
                    .filter(reason -> reason != null && !reason.isBlank())
                    .findFirst()
                    .orElse("Hết thời hạn thanh toán");
        }

        return new BookingOperationalInfoDto(
                reservationState,
                held,
                booked,
                released,
                expired,
                paymentAttempted,
                resolveAttentionCode(booking, now),
                resolveStateChangedAt(booking),
                reasonCode,
                reasonDetail);
    }

    private int countReservations(
            List<SeatReservation> reservations,
            SeatReservationStatus status) {
        return (int) reservations.stream()
                .filter(reservation -> reservation.getStatus() == status)
                .count();
    }

    private String resolveReservationState(
            int total,
            int held,
            int booked,
            int released,
            int expired) {
        if (total == 0) {
            return "NONE";
        }
        if (held == total) {
            return SeatReservationStatus.HELD.name();
        }
        if (booked == total) {
            return SeatReservationStatus.BOOKED.name();
        }
        if (released == total) {
            return SeatReservationStatus.RELEASED.name();
        }
        if (expired == total) {
            return SeatReservationStatus.EXPIRED.name();
        }
        return "MIXED";
    }

    private String resolveAttentionCode(Booking booking, Instant now) {
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            return null;
        }
        if (booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(now)) {
            return BookingAttentionFilter.OVERDUE.name();
        }
        if (booking.getPaymentStatus() == PaymentStatus.FAILED) {
            return BookingAttentionFilter.PAYMENT_FAILED.name();
        }
        if (booking.getExpiresAt() != null
                && !booking.getExpiresAt().isAfter(now.plus(5, ChronoUnit.MINUTES))) {
            return BookingAttentionFilter.EXPIRING_SOON.name();
        }
        return null;
    }

    private Instant resolveStateChangedAt(Booking booking) {
        return switch (booking.getBookingStatus()) {
            case CONFIRMED -> booking.getConfirmedAt();
            case COMPLETED -> booking.getCompletedAt();
            case CANCELLED -> booking.getCancelledAt();
            case EXPIRED -> booking.getExpiredAt();
            case REFUNDED -> booking.getRefundedAt();
            case PENDING_PAYMENT -> booking.getCreatedAt();
        };
    }

    @Override
    @Transactional
    public BookingAdminResponse updateBookingStatus(String publicId, UpdateBookingStatusRequest request) {
        if (publicId == null || publicId.trim().isEmpty()) {
            throw new BusinessException("INVALID_BOOKING_ID", "Booking public ID cannot be null or empty");
        }
        if (request == null || request.getStatus() == null) {
            throw new BusinessException("INVALID_REQUEST", "Target status is required");
        }

        MDC.put("bookingId", publicId);
        MDC.put("action", "ADMIN_CHANGE_STATUS");
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BookingNotFoundException(java.util.UUID.fromString(publicId)));

        BookingStatus oldStatus = booking.getBookingStatus();
        BookingStatus newStatus = request.getStatus();

        validateAdminLifecycleCommand(oldStatus, newStatus);
        if (lifecycleService != null) {
            Booking saved = lifecycleService.transition(booking, newStatus,
                    request.getReason(), "ADMIN");
            if (request.getNote() != null) {
                saved.setNote(request.getNote());
                saved = bookingRepository.save(saved);
            }
            return bookingMapper.toAdminResponse(saved);
        }
        statusTransitionService.validateTransition(oldStatus, newStatus);

        Instant now = Instant.now();
        if (newStatus == BookingStatus.CANCELLED) {
            booking.cancel("ADMIN_CANCEL", request.getReason(), now);
        } else {
            booking.changeStatus(newStatus, now);
        }

        if (request.getNote() != null) {
            booking.setNote(request.getNote());
        }

        Booking savedBooking = bookingRepository.save(booking);

        if (reservationService != null
                && (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED)) {
            reservationService.handleBookingStatusChange(savedBooking.getId(), newStatus,
                    request.getReason() != null ? request.getReason() : "Admin lifecycle status change");
        }

        historyService.saveHistory(savedBooking, oldStatus.name(), newStatus.name(), request.getReason(), "ADMIN", "ADMIN");
        auditService.logAudit(savedBooking.getId(), "ADMIN", "CHANGE_STATUS", "bookingStatus", oldStatus.name(), newStatus.name(), null, null, null, null);
        operationLogService.logOperation(savedBooking.getId(), "CHANGE_STATUS", "ADMIN", true, 0L, null, null, request.getReason());
        outboxService.createOutboxEvent("BOOKING", savedBooking.getId(), "BOOKING_" + newStatus.name(), savedBooking);

        // Preserve ticket rows and mark them with the appropriate terminal
        // state for audit/reconciliation.
        if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED || newStatus == BookingStatus.REFUNDED) {
            try {
                if (newStatus == BookingStatus.REFUNDED) {
                    ticketService.refundTickets(savedBooking.getId());
                } else {
                    ticketService.deleteTickets(savedBooking.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to update tickets for bookingId: {}", savedBooking.getId(), e);
            }
        }

        // Increment Metrics
        if (newStatus == BookingStatus.CONFIRMED) {
            bookingMetricsManager.incrementBookingConfirmed();
            bookingMetricsManager.incrementPaymentSuccess();
        } else if (newStatus == BookingStatus.EXPIRED) {
            bookingMetricsManager.incrementBookingExpired();
            bookingMetricsManager.incrementPaymentFailed();
        } else if (newStatus == BookingStatus.CANCELLED) {
            bookingMetricsManager.incrementBookingCancelled();
        }

        return bookingMapper.toAdminResponse(savedBooking);
    }

    private void validateAdminLifecycleCommand(
            BookingStatus currentStatus,
            BookingStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new BusinessException(
                    "SAME_STATUS_TRANSITION",
                    "Booking is already in status: " + targetStatus,
                    org.springframework.http.HttpStatus.CONFLICT);
        }
        if (targetStatus == BookingStatus.CONFIRMED) {
            throw new BusinessException(
                    "CONFIRM_VIA_PAYMENT_RESULT_REQUIRED",
                    "Booking confirmation is performed only by a validated Payment result",
                    org.springframework.http.HttpStatus.GONE);
        }
        if (targetStatus == BookingStatus.REFUNDED) {
            throw new BusinessException(
                    "REFUND_VIA_PAYMENT_RESULT_REQUIRED",
                    "Booking refund state is performed only by an authoritative Payment refund result",
                    org.springframework.http.HttpStatus.GONE);
        }
        if (targetStatus == BookingStatus.EXPIRED) {
            throw new BusinessException(
                    "BOOKING_EXPIRY_SYSTEM_OWNED",
                    "Booking expiration is owned by the stored deadline and expiration lifecycle",
                    org.springframework.http.HttpStatus.CONFLICT);
        }
        if (targetStatus == BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException(
                    "BOOKING_PENDING_STATE_IMMUTABLE",
                    "Admin cannot move a Booking back to pending payment",
                    org.springframework.http.HttpStatus.CONFLICT);
        }

        boolean allowed = (currentStatus == BookingStatus.PENDING_PAYMENT
                && targetStatus == BookingStatus.CANCELLED)
                || (currentStatus == BookingStatus.CONFIRMED
                && targetStatus == BookingStatus.COMPLETED);
        if (!allowed) {
            throw new BusinessException(
                    "ADMIN_LIFECYCLE_COMMAND_NOT_ALLOWED",
                    "Admin command is not allowed from " + currentStatus + " to " + targetStatus,
                    org.springframework.http.HttpStatus.CONFLICT);
        }
    }
}
