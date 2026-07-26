package com.lorafilm.booking.booking.service;

import com.lorafilm.booking.audit.service.BookingAuditService;
import com.lorafilm.booking.audit.service.BookingOperationLogService;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The single non-payment lifecycle coordinator.  Payment SUCCESS has an
 * additional atomic receiver because it must persist the payment event and
 * validate the stored amount/deadline before confirming.
 */
@Service
public class BookingLifecycleService {

    private final BookingRepository bookingRepository;
    private final BookingStatusTransitionService transitionService;
    private final BookingStatusHistoryService historyService;
    private final BookingAuditService auditService;
    private final BookingOperationLogService operationLogService;
    private final BookingOutboxService outboxService;
    private final BookingTicketService ticketService;
    private final SeatReservationService reservationService;
    private final SeatReservationRepository reservationRepository;
    private final BookingMetricsManager metricsManager;

    public BookingLifecycleService(
            BookingRepository bookingRepository,
            BookingStatusTransitionService transitionService,
            BookingStatusHistoryService historyService,
            BookingAuditService auditService,
            BookingOperationLogService operationLogService,
            BookingOutboxService outboxService,
            BookingTicketService ticketService,
            SeatReservationService reservationService,
            BookingMetricsManager metricsManager,
            SeatReservationRepository reservationRepository) {
        this.bookingRepository = bookingRepository;
        this.transitionService = transitionService;
        this.historyService = historyService;
        this.auditService = auditService;
        this.operationLogService = operationLogService;
        this.outboxService = outboxService;
        this.ticketService = ticketService;
        this.reservationService = reservationService;
        this.metricsManager = metricsManager;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Booking transition(Booking booking, BookingStatus targetStatus, String reason, String source) {
        if (booking == null || targetStatus == null) {
            throw new BusinessException("INVALID_STATUS", "Booking and target status are required");
        }
        if (targetStatus == BookingStatus.CONFIRMED) {
            throw new BusinessException("CONFIRM_VIA_PAYMENT_RESULT_REQUIRED",
                    "Booking confirmation is performed only by a validated Payment result",
                    HttpStatus.GONE);
        }

        BookingStatus previousStatus = booking.getBookingStatus();
        if (previousStatus == targetStatus) {
            return booking;
        }
        transitionService.validateTransition(previousStatus, targetStatus);
        Instant changedAt = Instant.now();
        if (targetStatus == BookingStatus.CANCELLED) {
            booking.cancel("LIFECYCLE", reason, changedAt);
        } else {
            booking.changeStatus(targetStatus, changedAt);
        }
        return persistTransition(booking, previousStatus, targetStatus, reason, source);
    }

    @Transactional
    public Booking cancel(Booking booking, String reasonCode, String reasonDetail, String source) {
        if (booking == null) {
            throw new BusinessException("INVALID_STATUS", "Booking is required");
        }
        BookingStatus previousStatus = booking.getBookingStatus();
        if (previousStatus == BookingStatus.CANCELLED) {
            return booking;
        }
        transitionService.validateTransition(previousStatus, BookingStatus.CANCELLED);
        booking.cancel(reasonCode, reasonDetail, Instant.now());
        return persistTransition(booking, previousStatus, BookingStatus.CANCELLED,
                reasonDetail == null ? reasonCode : reasonDetail, source);
    }

    @Transactional
    public Booking confirmPayment(Booking booking, Instant receiptAt, String source) {
        if (booking == null) {
            throw new BusinessException("INVALID_STATUS", "Booking is required");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getAmountLockedAt() == null
                || receiptAt == null
                || !receiptAt.isBefore(booking.getExpiresAt())) {
            throw new BusinessException("BOOKING_NOT_PAYABLE",
                    "Only an amount-locked, unexpired pending Booking can be confirmed",
                    HttpStatus.CONFLICT);
        }

        Instant now = Instant.now();
        java.util.List<SeatReservation> reservations =
                reservationRepository.findAllByBookingId(booking.getId());
        if (reservations.isEmpty() || reservations.stream().anyMatch(row ->
                row.getStatus() != SeatReservationStatus.HELD
                        || row.getExpiresAt() == null
                        || !row.getExpiresAt().isAfter(now))) {
            throw new BusinessException("BOOKING_RESERVATIONS_NOT_HELD",
                    "All Booking seats must remain HELD until payment succeeds",
                    HttpStatus.CONFLICT);
        }
        transitionService.validateTransition(booking.getBookingStatus(), BookingStatus.CONFIRMED);
        booking.changeStatus(BookingStatus.CONFIRMED, receiptAt);
        reservations.forEach(row -> row.setStatus(SeatReservationStatus.BOOKED));
        reservationRepository.saveAll(reservations);
        Booking saved = bookingRepository.save(booking);

        historyService.saveHistory(saved, BookingStatus.PENDING_PAYMENT.name(),
                BookingStatus.CONFIRMED.name(), "Authoritative payment result",
                source == null ? "PAYMENT_SERVICE" : source, "PAYMENT");
        auditService.logAudit(saved.getId(), "PAYMENT", "BOOKING_LIFECYCLE",
                "bookingStatus", BookingStatus.PENDING_PAYMENT.name(),
                BookingStatus.CONFIRMED.name(), null, null, null, null);
        operationLogService.logOperation(saved.getId(), "PAYMENT_CONFIRMATION",
                "PAYMENT", true, 0L, null, null, "HELD_TO_BOOKED");
        outboxService.createOutboxEvent("BOOKING", saved.getId(),
                "BOOKING_CONFIRMED", saved);
        if (saved.getFoodOrder() != null) {
            com.lorafilm.booking.food.event.FoodOrderConfirmedEvent foodEvent =
                    new com.lorafilm.booking.food.event.FoodOrderConfirmedEvent(
                            saved.getId().toString(),
                            saved.getFoodOrder().getPublicId(),
                            saved.getFoodOrder().getFinalAmount());
            outboxService.createOutboxEvent("FoodOrder", saved.getFoodOrder().getId(),
                    "FOOD_ORDER_CONFIRMED", foodEvent);
        }
        ticketService.generateTicketsForConfirmedBooking(saved.getId());
        metricsManager.incrementBookingConfirmed();
        metricsManager.incrementPaymentSuccess();
        return saved;
    }

    private Booking persistTransition(Booking booking, BookingStatus previousStatus,
                                      BookingStatus targetStatus, String reason, String source) {
        Booking saved = bookingRepository.save(booking);

        if (targetStatus == BookingStatus.CANCELLED || targetStatus == BookingStatus.EXPIRED) {
            reservationService.handleBookingStatusChange(saved.getId(), targetStatus, reason);
        } else if (targetStatus == BookingStatus.REFUNDED) {
            // BOOKED capacity is intentionally retained after a refund marker.
            reservationService.handleBookingStatusChange(saved.getId(), targetStatus, reason);
        }

        historyService.saveHistory(saved, previousStatus.name(), targetStatus.name(),
                reason, source == null ? "BOOKING_LIFECYCLE" : source, "SYSTEM");
        auditService.logAudit(saved.getId(), "SYSTEM", "BOOKING_LIFECYCLE",
                "bookingStatus", previousStatus.name(), targetStatus.name(),
                null, null, null, null);
        operationLogService.logOperation(saved.getId(), "BOOKING_LIFECYCLE", "SYSTEM",
                true, 0L, null, null, reason);
        outboxService.createOutboxEvent("BOOKING", saved.getId(),
                "BOOKING_" + targetStatus.name(), saved);

        if (targetStatus == BookingStatus.CANCELLED
                || targetStatus == BookingStatus.EXPIRED) {
            ticketService.deleteTickets(saved.getId());
        } else if (targetStatus == BookingStatus.REFUNDED) {
            ticketService.refundTickets(saved.getId());
        }
        if (targetStatus == BookingStatus.EXPIRED) {
            metricsManager.incrementBookingExpired();
            metricsManager.incrementPaymentFailed();
        } else if (targetStatus == BookingStatus.CANCELLED) {
            metricsManager.incrementBookingCancelled();
        }
        return saved;
    }
}
