package com.lorafilm.booking.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.LatePaymentSuccessException;
import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import com.lorafilm.booking.payment.enums.PaymentEventStatus;
import com.lorafilm.booking.payment.enums.PaymentEventType;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import com.lorafilm.booking.infrastructure.enums.ReconciliationStatus;
import com.lorafilm.booking.infrastructure.repository.BookingReconciliationTaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class InternalBookingPaymentServiceImpl implements InternalBookingPaymentService {

    private final BookingRepository bookingRepository;
    private final BookingPriceSnapshotRepository priceSnapshotRepository;
    private final BookingPaymentEventRepository paymentEventRepository;
    private final BookingStatusHistoryService historyService;
    private final ObjectMapper objectMapper;
    private final com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService;
    private final com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService;
    private final com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager;
    private final SeatReservationRepository reservationRepository;
    private final BookingReconciliationTaskRepository reconciliationTaskRepository;
    private final BookingLifecycleService lifecycleService;

    @Autowired
    public InternalBookingPaymentServiceImpl(BookingRepository bookingRepository,
                                             BookingPriceSnapshotRepository priceSnapshotRepository,
                                             BookingPaymentEventRepository paymentEventRepository,
                                             BookingStatusHistoryService historyService,
                                             ObjectMapper objectMapper,
                                             com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService,
                                             com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
                                             com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager,
                                             SeatReservationRepository reservationRepository,
                                             BookingReconciliationTaskRepository reconciliationTaskRepository,
                                             BookingLifecycleService lifecycleService) {
        this.bookingRepository = bookingRepository;
        this.priceSnapshotRepository = priceSnapshotRepository;
        this.paymentEventRepository = paymentEventRepository;
        this.historyService = historyService;
        this.objectMapper = objectMapper;
        this.bookingTicketService = bookingTicketService;
        this.outboxService = outboxService;
        this.metricsManager = metricsManager;
        this.reservationRepository = reservationRepository;
        this.reconciliationTaskRepository = reconciliationTaskRepository;
        this.lifecycleService = lifecycleService;
    }

    public InternalBookingPaymentServiceImpl(BookingRepository bookingRepository,
                                             BookingPriceSnapshotRepository priceSnapshotRepository,
                                             BookingPaymentEventRepository paymentEventRepository,
                                             BookingStatusHistoryService historyService,
                                             ObjectMapper objectMapper,
                                             com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService,
                                             com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
                                             com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager) {
        this(bookingRepository, priceSnapshotRepository, paymentEventRepository, historyService,
                objectMapper, bookingTicketService, outboxService, metricsManager, null, null, null);
    }

    /** Compatibility constructor for tests/callers that provide the repositories explicitly. */
    public InternalBookingPaymentServiceImpl(BookingRepository bookingRepository,
                                             BookingPriceSnapshotRepository priceSnapshotRepository,
                                             BookingPaymentEventRepository paymentEventRepository,
                                             BookingStatusHistoryService historyService,
                                             ObjectMapper objectMapper,
                                             com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService,
                                             com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
                                             com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager,
                                             SeatReservationRepository reservationRepository,
                                             BookingReconciliationTaskRepository reconciliationTaskRepository) {
        this(bookingRepository, priceSnapshotRepository, paymentEventRepository, historyService,
                objectMapper, bookingTicketService, outboxService, metricsManager,
                reservationRepository, reconciliationTaskRepository, null);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getPaymentContext(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        Instant now = Instant.now();
        boolean payable = booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT
                && booking.getExpiresAt() != null
                && booking.getExpiresAt().isAfter(now)
                && booking.getAmountLockedAt() != null
                && booking.getFinalAmount() != null
                && booking.getFinalAmount().signum() > 0
                && hasLiveHeldReservations(booking.getId(), now);
        if (!payable) {
            throw new BusinessException(
                    "BOOKING_NOT_PAYABLE",
                    "Booking is not pending payment or its payment deadline has passed",
                    HttpStatus.CONFLICT);
        }
        BookingPriceSnapshotPayload snapshot = readSnapshot(bookingId);
        return new InternalPaymentContextResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getBookingStatus().name(),
                true,
                booking.getFinalAmount(),
                booking.getCurrency(),
                LocalDateTime.ofInstant(booking.getExpiresAt(), ZoneOffset.UTC),
                new InternalPaymentContextResponse.AnalyticsSnapshot(
                        snapshot.movieId(), snapshot.movieTitle(), snapshot.seats().size()));
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getPaymentContext(String bookingPublicId) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> new BookingNotFoundException(bookingPublicId));
        return getPaymentContext(booking.getId());
    }

    @Override
    @Transactional(noRollbackFor = LatePaymentSuccessException.class)
    public InternalPaymentResultResponse recordPaymentResult(Long bookingId,
                                                             InternalPaymentResultRequest request) {
        String result = normalizeResult(request.result());
        Booking booking = bookingRepository.findByIdForPaymentUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        BookingPaymentEvent replay = paymentEventRepository.findByPublicId(request.eventId()).orElse(null);
        if (replay != null) {
            if (!replay.getBooking().getId().equals(bookingId)
                    || replay.getAmount().compareTo(request.amount()) != 0
                    || !replay.getCurrency().equals(request.currency())
                    || !Objects.equals(replay.getPaymentId(), request.paymentId())
                    || replay.getEventType() != eventType(result)
                    || !Objects.equals(replay.getPaymentMethod(), request.paymentMethod())) {
                throw new BusinessException(
                        "PAYMENT_EVENT_ID_REUSED",
                        "Payment event ID was reused with a different payload",
                        HttpStatus.CONFLICT);
            }
            return response(booking, request.eventId(), true);
        }

        validateAmountAndCurrency(booking, request);
        Instant occurredAt = request.occurredAt() == null
                ? Instant.now() : request.occurredAt().toInstant(ZoneOffset.UTC);

        if ("SUCCESS".equals(result)) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                createReconciliation(booking, request, "CONFLICTING_SUCCESS_AFTER_CONFIRMATION");
                throw new BusinessException(
                        "BOOKING_ALREADY_PAID",
                        "Booking is already confirmed by another payment",
                        HttpStatus.CONFLICT);
            }
            if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                    || booking.getExpiresAt() == null
                    || booking.getAmountLockedAt() == null
                    || !Instant.now().isBefore(booking.getExpiresAt())) {
                createReconciliation(booking, request, "LATE_OR_NON_PENDING_SUCCESS");
                throw new LatePaymentSuccessException(
                        "Expired or non-pending Booking cannot be confirmed");
            }
            booking.setPaymentStatus(PaymentStatus.SUCCESS);
            booking.setPaymentReference(reference(request));
            booking.setPaymentMethodSnapshot(request.paymentMethod());
            if (lifecycleService != null) {
                booking = lifecycleService.confirmPayment(
                        booking, Instant.now(), "PAYMENT_SERVICE");
            } else {
                // Compatibility constructor fallback for isolated legacy tests.
                booking.changeStatus(BookingStatus.CONFIRMED, Instant.now());
                confirmReservations(booking.getId(), Instant.now());
                historyService.saveHistory(
                        booking, BookingStatus.PENDING_PAYMENT.name(), BookingStatus.CONFIRMED.name(),
                        "Authoritative payment result", "PAYMENT_SERVICE", String.valueOf(request.paymentId()));
                bookingTicketService.generateTicketsForConfirmedBooking(booking.getId());
                metricsManager.incrementBookingConfirmed();
                metricsManager.incrementPaymentSuccess();
            }
        } else if ("FAILED".equals(result) || "CANCELLED".equals(result) || "TIMEOUT".equals(result)) {
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                // Preserve the confirmed Booking and record the later failure
                // for audit/deduplication.  A failed delivery can never
                // downgrade a successful payment.
            } else if (booking.getBookingStatus() == BookingStatus.REFUNDED
                    || booking.getBookingStatus() == BookingStatus.COMPLETED) {
                // Terminal post-confirmation states are likewise immutable.
            } else {
                booking.setPaymentStatus(PaymentStatus.FAILED);
            }
            if ("TIMEOUT".equals(result)) {
                metricsManager.incrementPaymentFailed();
            }
        }

        bookingRepository.save(booking);
        BookingPaymentEvent event = toEvent(booking, request, result, occurredAt);
        paymentEventRepository.save(event);
        return response(booking, request.eventId(), false);
    }

    @Override
    @Transactional(noRollbackFor = LatePaymentSuccessException.class)
    public InternalPaymentResultResponse recordPaymentResult(String bookingPublicId,
                                                             InternalPaymentResultRequest request) {
        Booking booking = bookingRepository.findByPublicIdWithLock(bookingPublicId)
                .orElseThrow(() -> new BookingNotFoundException(bookingPublicId));
        return recordPaymentResult(booking.getId(), request);
    }

    @Override
    @Transactional
    public void finalizeCheckout(Long bookingId) {
        Booking booking = bookingRepository.findByIdForPaymentUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("BOOKING_NOT_PENDING", "Only pending Bookings can be finalized", HttpStatus.CONFLICT);
        }
        if (booking.getExpiresAt() == null || !Instant.now().isBefore(booking.getExpiresAt())) {
            throw new BusinessException("BOOKING_EXPIRED", "The Booking payment deadline has passed", HttpStatus.CONFLICT);
        }
        booking.lockAmount(Instant.now());
        bookingRepository.save(booking);
    }

    private void confirmReservations(Long bookingId, Instant now) {
        if (reservationRepository == null) {
            return;
        }
        List<SeatReservation> reservations = reservationRepository.findAllByBookingId(bookingId);
        if (reservations.isEmpty() || reservations.stream().anyMatch(reservation ->
                reservation.getStatus() != SeatReservationStatus.HELD
                        || reservation.getExpiresAt() == null
                        || !reservation.getExpiresAt().isAfter(now))) {
            throw new BusinessException("BOOKING_RESERVATIONS_NOT_HELD",
                    "All Booking seats must remain HELD until payment succeeds", HttpStatus.CONFLICT);
        }
        reservations.forEach(reservation -> reservation.setStatus(SeatReservationStatus.BOOKED));
        reservationRepository.saveAll(reservations);
    }

    private boolean hasLiveHeldReservations(Long bookingId, Instant now) {
        if (reservationRepository == null) {
            return true;
        }
        List<SeatReservation> reservations = reservationRepository.findAllByBookingId(bookingId);
        return !reservations.isEmpty() && reservations.stream().allMatch(reservation ->
                reservation.getStatus() == SeatReservationStatus.HELD
                        && reservation.getExpiresAt() != null
                        && reservation.getExpiresAt().isAfter(now));
    }

    private void createReconciliation(Booking booking, InternalPaymentResultRequest request, String reason) {
        if (reconciliationTaskRepository == null) {
            return;
        }
        BookingReconciliationTask task = new BookingReconciliationTask();
        task.setPublicId(java.util.UUID.randomUUID().toString());
        task.setBooking(booking);
        task.setPaymentReference(reference(request));
        task.setExpectedAmount(booking.getFinalAmount());
        task.setActualAmount(request.amount());
        task.setReconciliationStatus(ReconciliationStatus.PENDING);
        task.setReason(reason);
        task.setCheckedAt(Instant.now());
        reconciliationTaskRepository.save(task);
    }

    private BookingPaymentEvent toEvent(Booking booking,
                                        InternalPaymentResultRequest request,
                                        String result,
                                        Instant occurredAt) {
        BookingPaymentEvent event = new BookingPaymentEvent();
        event.setPublicId(request.eventId());
        event.setBooking(booking);
        event.setPaymentId(request.paymentId());
        event.setTransactionId(request.paymentTransactionCode());
        event.setGatewayTransactionId(request.externalTransactionId());
        event.setPaymentMethod(request.paymentMethod());
        event.setEventType(eventType(result));
        event.setAmount(request.amount());
        event.setCurrency(request.currency());
        event.setStatus("SUCCESS".equals(result) ? PaymentEventStatus.SUCCESS
                : "PENDING".equals(result) ? PaymentEventStatus.PENDING : PaymentEventStatus.FAILED);
        event.setOccurredAt(occurredAt);
        try {
            event.setRequestPayload(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Cannot serialize payment result",
                    HttpStatus.BAD_REQUEST);
        }
        return event;
    }

    private PaymentEventType eventType(String result) {
        return switch (result) {
            case "SUCCESS" -> PaymentEventType.PAYMENT_SUCCESS;
            case "FAILED" -> PaymentEventType.PAYMENT_FAILED;
            case "TIMEOUT" -> PaymentEventType.PAYMENT_TIMEOUT;
            case "CANCELLED" -> PaymentEventType.PAYMENT_CANCELLED;
            default -> PaymentEventType.PAYMENT_PENDING;
        };
    }

    private String normalizeResult(String result) {
        String normalized = result == null ? "" : result.trim().toUpperCase();
        if (!List.of("SUCCESS", "FAILED", "CANCELLED", "TIMEOUT", "PENDING").contains(normalized)) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Unsupported payment result: " + result,
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private void validateAmountAndCurrency(Booking booking, InternalPaymentResultRequest request) {
        BigDecimal amount = request.amount();
        if (amount == null || booking.getFinalAmount().compareTo(amount) != 0
                || request.currency() == null || !booking.getCurrency().equals(request.currency())) {
            throw new BusinessException(
                    "PAYMENT_AMOUNT_MISMATCH",
                    "Payment result amount/currency does not match the Booking",
                    HttpStatus.CONFLICT);
        }
    }

    private BookingPriceSnapshotPayload readSnapshot(Long bookingId) {
        BookingPriceSnapshot snapshot = priceSnapshotRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BusinessException(
                        "BOOKING_PRICE_SNAPSHOT_MISSING",
                        "Authoritative Booking price snapshot is missing",
                        HttpStatus.CONFLICT));
        try {
            return objectMapper.readValue(
                    snapshot.getPricingBreakdownJson(), BookingPriceSnapshotPayload.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "BOOKING_PRICE_SNAPSHOT_INVALID",
                    "Authoritative Booking price snapshot is unreadable",
                    HttpStatus.CONFLICT);
        }
    }

    private String reference(InternalPaymentResultRequest request) {
        return request.paymentTransactionCode() == null || request.paymentTransactionCode().isBlank()
                ? String.valueOf(request.paymentId()) : request.paymentTransactionCode();
    }

    private InternalPaymentResultResponse response(Booking booking, String eventId, boolean idempotent) {
        return new InternalPaymentResultResponse(
                booking.getId(), eventId, booking.getBookingStatus().name(),
                booking.getPaymentStatus().name(), idempotent);
    }
}
