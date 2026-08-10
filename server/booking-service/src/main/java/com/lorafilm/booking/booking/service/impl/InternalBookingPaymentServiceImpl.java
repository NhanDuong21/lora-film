package com.lorafilm.booking.booking.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.BookingPriceSnapshotPayload;
import com.lorafilm.booking.booking.client.ScoreRedemptionClient;
import com.lorafilm.booking.booking.client.PromotionReservationClient;
import com.lorafilm.booking.booking.dto.request.InternalPaymentResultRequest;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import com.lorafilm.booking.booking.dto.response.InternalPaymentResultResponse;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import com.lorafilm.booking.booking.entity.BookingSnapshot;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import com.lorafilm.booking.booking.repository.BookingPriceSnapshotRepository;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.booking.repository.BookingSnapshotRepository;
import com.lorafilm.booking.booking.service.BookingLifecycleService;
import com.lorafilm.booking.booking.service.BookingStatusHistoryService;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.common.exception.BookingNotFoundException;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.PaymentResultConflictException;
import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import com.lorafilm.booking.infrastructure.enums.ReconciliationStatus;
import com.lorafilm.booking.infrastructure.repository.BookingReconciliationTaskRepository;
import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import com.lorafilm.booking.payment.entity.BookingRefund;
import com.lorafilm.booking.payment.enums.PaymentEventStatus;
import com.lorafilm.booking.payment.enums.PaymentEventType;
import com.lorafilm.booking.payment.enums.RefundStatus;
import com.lorafilm.booking.payment.repository.BookingPaymentEventRepository;
import com.lorafilm.booking.payment.repository.BookingRefundRepository;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import com.lorafilm.booking.reservation.repository.SeatReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class InternalBookingPaymentServiceImpl implements InternalBookingPaymentService {

    private static final String OUTCOME_ACCEPTED = "ACCEPTED";
    private static final String OUTCOME_RECONCILIATION_REQUIRED = "RECONCILIATION_REQUIRED";

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
    private final BookingRefundRepository refundRepository;
    private final BookingLifecycleService lifecycleService;
    private BookingSnapshotRepository bookingSnapshotRepository;
    private ScoreRedemptionClient scoreRedemptionClient;
    private PromotionReservationClient promotionReservationClient;

    @Autowired
    public InternalBookingPaymentServiceImpl(
            BookingRepository bookingRepository,
            BookingPriceSnapshotRepository priceSnapshotRepository,
            BookingPaymentEventRepository paymentEventRepository,
            BookingStatusHistoryService historyService,
            ObjectMapper objectMapper,
            com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService,
            com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
            com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager,
            SeatReservationRepository reservationRepository,
            BookingReconciliationTaskRepository reconciliationTaskRepository,
            BookingRefundRepository refundRepository,
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
        this.refundRepository = refundRepository;
        this.lifecycleService = lifecycleService;
    }

    public InternalBookingPaymentServiceImpl(
            BookingRepository bookingRepository,
            BookingPriceSnapshotRepository priceSnapshotRepository,
            BookingPaymentEventRepository paymentEventRepository,
            BookingStatusHistoryService historyService,
            ObjectMapper objectMapper,
            com.lorafilm.booking.booking.service.BookingTicketService bookingTicketService,
            com.lorafilm.booking.infrastructure.service.BookingOutboxService outboxService,
            com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager metricsManager) {
        this(bookingRepository, priceSnapshotRepository, paymentEventRepository, historyService,
                objectMapper, bookingTicketService, outboxService, metricsManager,
                null, null, null, null);
    }

    /** Compatibility constructor for focused tests that provide reservation repositories. */
    public InternalBookingPaymentServiceImpl(
            BookingRepository bookingRepository,
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
                reservationRepository, reconciliationTaskRepository, null, null);
    }

    @Autowired(required = false)
    public void setScoreRedemptionClient(ScoreRedemptionClient service) {
        this.scoreRedemptionClient = service;
    }

    @Autowired(required = false)
    public void setPromotionReservationClient(PromotionReservationClient service) {
        this.promotionReservationClient = service;
    }

    @Autowired(required = false)
    public void setBookingSnapshotRepository(BookingSnapshotRepository repository) {
        this.bookingSnapshotRepository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getPaymentContext(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        return buildPaymentContext(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getPaymentContext(String bookingPublicId) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(bookingPublicId));
        return buildPaymentContext(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getPaymentContextByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(bookingCode));
        return buildPaymentContext(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getScoreRedemptionContext(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        return buildScoreRedemptionContext(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalPaymentContextResponse getScoreRedemptionContext(String bookingPublicId) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .orElseThrow(() -> new BookingNotFoundException(bookingPublicId));
        return buildScoreRedemptionContext(booking);
    }

    private InternalPaymentContextResponse buildPaymentContext(Booking booking) {
        Instant now = Instant.now();
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException(
                    "BOOKING_CANCELLED",
                    "Đơn đặt vé đã được hủy và ghế đã được trả lại",
                    HttpStatus.CONFLICT);
        }
        if (booking.getBookingStatus() == BookingStatus.EXPIRED
                || booking.getExpiresAt() == null
                || !booking.getExpiresAt().isAfter(now)) {
            throw new BusinessException(
                    "BOOKING_PAYMENT_DEADLINE_EXPIRED",
                    "Đơn đặt vé đã hết thời gian thanh toán",
                    HttpStatus.CONFLICT);
        }
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED
                || booking.getBookingStatus() == BookingStatus.COMPLETED
                || booking.getBookingStatus() == BookingStatus.REFUNDED) {
            throw new BusinessException(
                    "BOOKING_ALREADY_PAID",
                    "Đơn đặt vé đã được thanh toán",
                    HttpStatus.CONFLICT);
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException(
                    "BOOKING_NOT_PAYABLE",
                    "Trạng thái hiện tại của đơn không cho phép thanh toán",
                    HttpStatus.CONFLICT);
        }
        if (booking.getAmountLockedAt() == null
                || booking.getFinalAmount() == null
                || booking.getFinalAmount().signum() <= 0) {
            throw new BusinessException(
                    "BOOKING_AMOUNT_NOT_LOCKED",
                    "Đơn chưa được chốt số tiền thanh toán",
                    HttpStatus.CONFLICT);
        }
        if (!hasLiveHeldReservations(booking.getId(), now)) {
            throw new BusinessException(
                    "BOOKING_SEATS_NOT_HELD",
                    "Ghế của đơn không còn được giữ",
                    HttpStatus.CONFLICT);
        }
        BookingPriceSnapshotPayload snapshot = readSnapshot(booking.getId());
        return new InternalPaymentContextResponse(
                booking.getId(),
                booking.getPublicId(),
                booking.getUserId(),
                booking.getBookingStatus().name(),
                true,
                booking.getFinalAmount(),
                normalizeCurrency(booking.getCurrency()),
                booking.getAmountLockedAt(),
                booking.getExpiresAt(),
                booking.getPaymentMethodSnapshot(),
                buildAnalyticsSnapshot(booking, snapshot));
    }

    private InternalPaymentContextResponse buildScoreRedemptionContext(Booking booking) {
        Instant now = Instant.now();
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getExpiresAt() == null
                || !booking.getExpiresAt().isAfter(now)) {
            throw new BusinessException(
                    "BOOKING_NOT_REDEEMABLE",
                    "Booking is not open for score redemption",
                    HttpStatus.CONFLICT);
        }
        if (booking.getAmountLockedAt() != null || booking.getFinalAmount() == null
                || booking.getFinalAmount().signum() <= 0) {
            throw new BusinessException(
                    "BOOKING_SCORE_AMOUNT_LOCKED",
                    "Score redemption must be selected before the checkout amount is locked",
                    HttpStatus.CONFLICT);
        }
        if (!hasLiveHeldReservations(booking.getId(), now)) {
            throw new BusinessException(
                    "BOOKING_SEATS_NOT_HELD",
                    "Booking seats are no longer held",
                    HttpStatus.CONFLICT);
        }
        BookingPriceSnapshotPayload snapshot = readSnapshot(booking.getId());
        return new InternalPaymentContextResponse(
                booking.getId(),
                booking.getPublicId(),
                booking.getUserId(),
                booking.getBookingStatus().name(),
                true,
                booking.getFinalAmount(),
                normalizeCurrency(booking.getCurrency()),
                booking.getAmountLockedAt(),
                booking.getExpiresAt(),
                booking.getPaymentMethodSnapshot(),
                buildAnalyticsSnapshot(booking, snapshot));
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    @Transactional(noRollbackFor = PaymentResultConflictException.class)
    public InternalPaymentResultResponse recordPaymentResult(
            Long bookingId,
            InternalPaymentResultRequest request) {
        Booking booking = bookingRepository.findByIdForPaymentUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(String.valueOf(bookingId)));
        return applyPaymentResult(booking, request);
    }

    @Override
    @Transactional(noRollbackFor = PaymentResultConflictException.class)
    public InternalPaymentResultResponse recordPaymentResult(
            String bookingPublicId,
            InternalPaymentResultRequest request) {
        Booking booking = bookingRepository.findByPublicIdWithLock(bookingPublicId)
                .orElseThrow(() -> new BookingNotFoundException(bookingPublicId));
        return applyPaymentResult(booking, request);
    }

    @Override
    @Transactional(noRollbackFor = PaymentResultConflictException.class)
    public InternalPaymentResultResponse recordRefundResult(
            String bookingPublicId,
            InternalPaymentResultRequest request) {
        if (request == null) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID", "Payment result is required", HttpStatus.BAD_REQUEST);
        }
        String result = normalizeResult(request.result());
        if (!"REFUND_SUCCESS".equals(result) && !"REFUND_FAILED".equals(result)) {
            throw new BusinessException(
                    "REFUND_RESULT_INVALID",
                    "Refund result endpoint accepts only REFUND_SUCCESS or REFUND_FAILED",
                    HttpStatus.BAD_REQUEST);
        }
        Booking booking = bookingRepository.findByPublicIdWithLock(bookingPublicId)
                .orElseThrow(() -> new BookingNotFoundException(bookingPublicId));
        return applyPaymentResult(booking, request);
    }

    private InternalPaymentResultResponse applyPaymentResult(
            Booking booking,
            InternalPaymentResultRequest request) {
        if (request == null) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID", "Payment result is required", HttpStatus.BAD_REQUEST);
        }

        validateContract(request);
        String result = normalizeResult(request.result());
        String fingerprint = payloadFingerprint(request, result);
        BookingPaymentEvent existing = paymentEventRepository.findByPublicId(request.eventId()).orElse(null);
        if (existing != null) {
            return replay(existing, booking, request, result, fingerprint);
        }

        Instant receiptAt = Instant.now();
        Instant occurredAt = request.occurredAt() == null ? receiptAt : request.occurredAt();
        BookingPaymentEvent event = toEvent(booking, request, result, occurredAt, fingerprint);

        if (!amountAndCurrencyMatch(booking, request, result)) {
            rejectWithReconciliation(
                    booking,
                    event,
                    request,
                    "PAYMENT_AMOUNT_MISMATCH",
                    "Payment result amount/currency does not match the locked Booking amount",
                    "AMOUNT_OR_CURRENCY_MISMATCH");
        }

        switch (result) {
            case "SUCCESS" -> applySuccess(booking, event, request, receiptAt);
            case "FAILED", "CANCELLED", "TIMEOUT" ->
                    applyUnsuccessfulAttempt(booking, result);
            case "REFUND_SUCCESS" -> applyRefundSuccess(booking, event, request, receiptAt);
            case "REFUND_FAILED" -> {
                // A failed settlement attempt is audit-only for Booking.
            }
            case "PENDING" -> {
                // An attempt can be pending without changing the authoritative
                // Booking lifecycle or its original deadline.
            }
            default -> throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Unsupported payment result: " + result,
                    HttpStatus.BAD_REQUEST);
        }

        bookingRepository.save(booking);
        InternalPaymentResultResponse response = response(
                booking, request, event, true, false, null);
        event.setProcessingOutcome(OUTCOME_ACCEPTED);
        event.setResponsePayload(writeJson(response));
        paymentEventRepository.saveAndFlush(event);
        if ("REFUND_SUCCESS".equals(result) || "REFUND_FAILED".equals(result)) {
            persistRefundProjection(
                    booking,
                    event,
                    request,
                    "REFUND_SUCCESS".equals(result) ? RefundStatus.SUCCESS : RefundStatus.FAILED,
                    receiptAt);
        }
        return response;
    }

    private void applySuccess(
            Booking booking,
            BookingPaymentEvent event,
            InternalPaymentResultRequest request,
            Instant receiptAt) {
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED
                || booking.getBookingStatus() == BookingStatus.COMPLETED
                || booking.getBookingStatus() == BookingStatus.REFUNDED) {
            rejectWithReconciliation(
                    booking,
                    event,
                    request,
                    "BOOKING_ALREADY_PAID",
                    "Booking was already confirmed by another Payment result",
                    "CONFLICTING_SUCCESS_AFTER_CONFIRMATION");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getExpiresAt() == null
                || booking.getAmountLockedAt() == null
                || !receiptAt.isBefore(booking.getExpiresAt())
                || !hasLiveHeldReservations(booking.getId(), receiptAt)) {
            rejectWithReconciliation(
                    booking,
                    event,
                    request,
                    "LATE_PAYMENT_SUCCESS",
                    "Expired, cancelled, or non-payable Booking cannot be confirmed",
                    "LATE_OR_NON_PENDING_SUCCESS");
        }
        String lockedProvider = normalizeUpper(booking.getPaymentMethodSnapshot());
        String actualProvider = normalizeUpper(request.paymentProvider());
        if (lockedProvider.isBlank() || !lockedProvider.equals(actualProvider)) {
            rejectWithReconciliation(
                    booking,
                    event,
                    request,
                    "PAYMENT_PROVIDER_MISMATCH",
                    "Payment provider does not match the provider locked at checkout",
                    "LOCKED_PROVIDER_MISMATCH");
        }

        if (booking.getPromotionReservationPublicId() != null) {
            if (promotionReservationClient == null) {
                throw new BusinessException(
                        "PROMOTION_SERVICE_UNAVAILABLE",
                        "Promotion reservation cannot be confirmed",
                        HttpStatus.BAD_GATEWAY);
            }
            String paymentPublicId = blankToNull(request.paymentPublicId());
            if (paymentPublicId == null) {
                paymentPublicId = UUID.nameUUIDFromBytes(
                        ("PAYMENT:" + request.paymentId()).getBytes(StandardCharsets.UTF_8)).toString();
            }
            promotionReservationClient.confirm(
                    booking.getPromotionReservationPublicId(),
                    paymentPublicId,
                    stablePromotionKey("confirm", request.eventId()));
        }

        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setPaymentReference(reference(request));
        booking.setPaymentProvider(actualProvider);
        if (lifecycleService != null) {
            lifecycleService.confirmPayment(booking, receiptAt, "PAYMENT_SERVICE");
        } else {
            // Compatibility fallback for isolated unit tests. Production uses
            // BookingLifecycleService as the only confirmation authority.
            booking.changeStatus(BookingStatus.CONFIRMED, receiptAt);
            confirmReservations(booking.getId(), receiptAt);
            historyService.saveHistory(
                    booking,
                    BookingStatus.PENDING_PAYMENT.name(),
                    BookingStatus.CONFIRMED.name(),
                    "Authoritative payment result",
                    "PAYMENT_SERVICE",
                    paymentIdentity(request));
            bookingTicketService.generateTicketsForConfirmedBooking(booking.getId());
            metricsManager.incrementBookingConfirmed();
            metricsManager.incrementPaymentSuccess();
        }
        if (booking.getScorePointsUsed() != null
                && booking.getScorePointsUsed() > 0
                && scoreRedemptionClient != null) {
            scoreRedemptionClient.commit(
                    booking.getId(),
                    booking.getScoreHoldCode(),
                    request.eventId(),
                    stableScoreKey("commit", request.eventId()));
        }
    }

    private void applyUnsuccessfulAttempt(Booking booking, String result) {
        if (booking.getBookingStatus() == BookingStatus.PENDING_PAYMENT) {
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }
        if ("TIMEOUT".equals(result)) {
            metricsManager.incrementPaymentFailed();
        }
    }

    private String stablePromotionKey(String purpose, String source) {
        return purpose + "-" + UUID.nameUUIDFromBytes(
                (purpose + ":" + source).getBytes(StandardCharsets.UTF_8));
    }

    private void applyRefundSuccess(
            Booking booking,
            BookingPaymentEvent event,
            InternalPaymentResultRequest request,
            Instant receiptAt) {
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            rejectWithReconciliation(
                    booking,
                    event,
                    request,
                    "BOOKING_NOT_REFUNDABLE",
                    "Only a confirmed Booking can accept a successful refund result",
                    "REFUND_SUCCESS_FOR_NON_CONFIRMED_BOOKING");
        }

        BigDecimal alreadyRefunded = successfulRefundedAmount(booking.getId());
        BigDecimal cumulativeRefund = alreadyRefunded.add(request.amount());
        if (cumulativeRefund.compareTo(booking.getFinalAmount()) == 0) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            if (lifecycleService != null) {
                lifecycleService.transition(
                        booking,
                        BookingStatus.REFUNDED,
                        "Authoritative full Payment refund result",
                        "PAYMENT_SERVICE");
            } else {
                booking.changeStatus(BookingStatus.REFUNDED, receiptAt);
            }
            if (booking.getScorePointsUsed() != null
                    && booking.getScorePointsUsed() > 0
                    && scoreRedemptionClient != null) {
                scoreRedemptionClient.refund(
                        booking.getUserId(),
                        booking.getId(),
                        booking.getScorePointsUsed(),
                        "Full Booking refund",
                        request.eventId(),
                        stableScoreKey("refund", request.eventId()));
            }
        }
    }

    private void persistRefundProjection(
            Booking booking,
            BookingPaymentEvent event,
            InternalPaymentResultRequest request,
            RefundStatus status,
            Instant receiptAt) {
        if (refundRepository == null) {
            return;
        }
        BookingRefund refund = new BookingRefund();
        refund.setPublicId(UUID.randomUUID().toString());
        refund.setRefundCode("RF-" + request.eventId());
        refund.setBooking(booking);
        refund.setPaymentEvent(event);
        refund.setRefundReasonCode("PAYMENT_SERVICE_RESULT");
        refund.setRefundReasonDetail(
                status == RefundStatus.SUCCESS
                        ? "Payment Service confirmed refund settlement"
                        : "Payment Service reported refund failure");
        refund.setRefundAmount(request.amount());
        refund.setRefundedBy("PAYMENT_SERVICE");
        refund.setRefundMethod(preferredPaymentLabel(request));
        refund.setRefundReference(reference(request));
        refund.setStatus(status);
        refund.setRequestedAt(request.occurredAt() == null ? receiptAt : request.occurredAt());
        if (status == RefundStatus.SUCCESS) {
            refund.setCompletedAt(receiptAt);
        }
        refundRepository.save(refund);
    }

    private InternalPaymentResultResponse replay(
            BookingPaymentEvent existing,
            Booking requestedBooking,
            InternalPaymentResultRequest request,
            String result,
            String fingerprint) {
        boolean sameBooking = existing.getBooking() != null
                && Objects.equals(existing.getBooking().getId(), requestedBooking.getId());
        boolean samePayload = existing.getPayloadHash() != null
                ? MessageDigest.isEqual(
                        existing.getPayloadHash().getBytes(StandardCharsets.UTF_8),
                        fingerprint.getBytes(StandardCharsets.UTF_8))
                : legacyPayloadMatches(existing, request, result);

        if (!sameBooking || !samePayload) {
            throw new BusinessException(
                    "PAYMENT_EVENT_ID_REUSED",
                    "Payment event ID was reused with a different normalized payload",
                    HttpStatus.CONFLICT);
        }

        InternalPaymentResultResponse stored = readStoredResponse(existing, requestedBooking, request);
        if (OUTCOME_RECONCILIATION_REQUIRED.equals(existing.getProcessingOutcome())) {
            throw new PaymentResultConflictException(
                    existing.getProcessingErrorCode() == null
                            ? "PAYMENT_RESULT_CONFLICT"
                            : existing.getProcessingErrorCode(),
                    "Payment result was already recorded and requires reconciliation",
                    existing.getReconciliationTaskPublicId());
        }
        return stored.asIdempotentReplay();
    }

    private void rejectWithReconciliation(
            Booking booking,
            BookingPaymentEvent event,
            InternalPaymentResultRequest request,
            String errorCode,
            String message,
            String reason) {
        event.setProcessingOutcome(OUTCOME_RECONCILIATION_REQUIRED);
        event.setProcessingErrorCode(errorCode);
        paymentEventRepository.saveAndFlush(event);

        BookingReconciliationTask task = createReconciliation(booking, event, request, reason);
        String taskPublicId = task == null ? null : task.getPublicId();
        event.setReconciliationTaskPublicId(taskPublicId);
        InternalPaymentResultResponse rejected = response(
                booking, request, event, false, true, taskPublicId);
        event.setResponsePayload(writeJson(rejected));
        paymentEventRepository.save(event);

        throw new PaymentResultConflictException(errorCode, message, taskPublicId);
    }

    private BookingReconciliationTask createReconciliation(
            Booking booking,
            BookingPaymentEvent event,
            InternalPaymentResultRequest request,
            String reason) {
        if (reconciliationTaskRepository == null) {
            return null;
        }
        BookingReconciliationTask task = new BookingReconciliationTask();
        task.setPublicId(UUID.randomUUID().toString());
        task.setBooking(booking);
        task.setPaymentEvent(event);
        task.setPaymentReference(reference(request));
        task.setExpectedAmount(booking.getFinalAmount());
        task.setActualAmount(request.amount());
        task.setExpectedCurrency(normalizeCurrency(booking.getCurrency()));
        task.setActualCurrency(normalizeCurrency(request.currency()));
        task.setReconciliationStatus(ReconciliationStatus.PENDING);
        task.setReason(reason);
        return reconciliationTaskRepository.save(task);
    }

    private BookingPaymentEvent toEvent(
            Booking booking,
            InternalPaymentResultRequest request,
            String result,
            Instant occurredAt,
            String fingerprint) {
        BookingPaymentEvent event = new BookingPaymentEvent();
        event.setPublicId(request.eventId());
        event.setBooking(booking);
        event.setSchemaVersion(request.schemaVersion());
        event.setPaymentId(request.paymentId());
        event.setPaymentPublicId(blankToNull(request.paymentPublicId()));
        event.setTransactionId(blankToNull(request.paymentTransactionCode()));
        event.setGatewayTransactionId(blankToNull(request.externalTransactionId()));
        event.setPaymentProvider(normalizeUpper(request.paymentProvider()));
        event.setPaymentMethod(normalizeUpper(request.paymentMethod()));
        event.setEventType(eventType(result));
        event.setAmount(request.amount());
        event.setCurrency(normalizeCurrency(request.currency()));
        event.setStatus(eventStatus(result));
        event.setOccurredAt(occurredAt);
        event.setPayloadHash(fingerprint);
        event.setRequestPayload(writeJson(request));
        return event;
    }

    private InternalPaymentResultResponse response(
            Booking booking,
            InternalPaymentResultRequest request,
            BookingPaymentEvent event,
            boolean accepted,
            boolean reconciliationRequired,
            String reconciliationTaskPublicId) {
        return new InternalPaymentResultResponse(
                booking.getId(),
                booking.getPublicId(),
                request.paymentId(),
                blankToNull(request.paymentPublicId()),
                event.getPublicId(),
                booking.getBookingStatus().name(),
                booking.getPaymentStatus().name(),
                accepted,
                false,
                reconciliationRequired,
                reconciliationTaskPublicId);
    }

    private InternalPaymentResultResponse readStoredResponse(
            BookingPaymentEvent event,
            Booking booking,
            InternalPaymentResultRequest request) {
        if (event.getResponsePayload() != null && !event.getResponsePayload().isBlank()) {
            try {
                return objectMapper.readValue(
                        event.getResponsePayload(), InternalPaymentResultResponse.class);
            } catch (JsonProcessingException ignored) {
                // Legacy/corrupt response snapshots fall back to the immutable
                // event and current Booking state without reapplying effects.
            }
        }
        return response(
                booking,
                request,
                event,
                !OUTCOME_RECONCILIATION_REQUIRED.equals(event.getProcessingOutcome()),
                OUTCOME_RECONCILIATION_REQUIRED.equals(event.getProcessingOutcome()),
                event.getReconciliationTaskPublicId());
    }

    private String payloadFingerprint(InternalPaymentResultRequest request, String result) {
        String canonical = String.join(
                "\u001f",
                normalize(request.schemaVersion()),
                request.paymentId() == null ? "" : request.paymentId().toString(),
                normalizeLower(request.paymentPublicId()),
                normalize(request.paymentTransactionCode()),
                normalizeUpper(request.paymentProvider()),
                normalizeUpper(request.paymentMethod()),
                result,
                normalizeAmount(request.amount()),
                normalizeCurrency(request.currency()),
                request.occurredAt() == null ? "" : request.occurredAt().toString(),
                normalize(request.externalTransactionId()));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean legacyPayloadMatches(
            BookingPaymentEvent event,
            InternalPaymentResultRequest request,
            String result) {
        return event.getAmount() != null
                && request.amount() != null
                && event.getAmount().compareTo(request.amount()) == 0
                && Objects.equals(
                        normalizeCurrency(event.getCurrency()),
                        normalizeCurrency(request.currency()))
                && Objects.equals(event.getPaymentId(), request.paymentId())
                && Objects.equals(
                        blankToNull(event.getPaymentPublicId()),
                        blankToNull(request.paymentPublicId()))
                && event.getEventType() == eventType(result)
                && Objects.equals(
                        normalizeUpper(event.getPaymentProvider()),
                        normalizeUpper(request.paymentProvider()))
                && Objects.equals(
                        normalizeUpper(event.getPaymentMethod()),
                        normalizeUpper(request.paymentMethod()))
                && Objects.equals(
                        blankToNull(event.getTransactionId()),
                        blankToNull(request.paymentTransactionCode()))
                && Objects.equals(
                        blankToNull(event.getGatewayTransactionId()),
                        blankToNull(request.externalTransactionId()));
    }

    private boolean amountAndCurrencyMatch(
            Booking booking,
            InternalPaymentResultRequest request,
            String result) {
        if (request.amount() == null
                || request.amount().compareTo(BigDecimal.ZERO) <= 0
                || booking.getFinalAmount() == null
                || !Objects.equals(
                    normalizeCurrency(booking.getCurrency()),
                    normalizeCurrency(request.currency()))) {
            return false;
        }
        if (!"REFUND_SUCCESS".equals(result) && !"REFUND_FAILED".equals(result)) {
            return booking.getFinalAmount().compareTo(request.amount()) == 0;
        }
        BigDecimal alreadyRefunded = successfulRefundedAmount(booking.getId());
        return alreadyRefunded.add(request.amount())
                .compareTo(booking.getFinalAmount()) <= 0;
    }

    private BigDecimal successfulRefundedAmount(Long bookingId) {
        if (refundRepository == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = refundRepository.sumSuccessfulAmountByBookingId(bookingId);
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private PaymentEventType eventType(String result) {
        return switch (result) {
            case "SUCCESS" -> PaymentEventType.PAYMENT_SUCCESS;
            case "FAILED" -> PaymentEventType.PAYMENT_FAILED;
            case "TIMEOUT" -> PaymentEventType.PAYMENT_TIMEOUT;
            case "CANCELLED" -> PaymentEventType.PAYMENT_CANCELLED;
            case "REFUND_SUCCESS" -> PaymentEventType.REFUND_SUCCESS;
            case "REFUND_FAILED" -> PaymentEventType.REFUND_FAILED;
            default -> PaymentEventType.PAYMENT_PENDING;
        };
    }

    private PaymentEventStatus eventStatus(String result) {
        return switch (result) {
            case "SUCCESS", "REFUND_SUCCESS" -> PaymentEventStatus.SUCCESS;
            case "PENDING" -> PaymentEventStatus.PENDING;
            default -> PaymentEventStatus.FAILED;
        };
    }

    private String normalizeResult(String result) {
        String normalized = normalizeUpper(result);
        if (!List.of(
                "SUCCESS",
                "FAILED",
                "CANCELLED",
                "TIMEOUT",
                "PENDING",
                "REFUND_SUCCESS",
                "REFUND_FAILED").contains(normalized)) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Unsupported payment result: " + result,
                    HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    /**
     * Enforce the same contract for HTTP and alternate delivery (outbox/Kafka).
     * Bean Validation at the controller boundary alone is insufficient because
     * event consumers call this service directly.
     */
    private void validateContract(InternalPaymentResultRequest request) {
        try {
            UUID.fromString(normalize(request.eventId()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "PAYMENT_EVENT_ID_INVALID",
                    "Payment event ID must be a UUID",
                    HttpStatus.BAD_REQUEST);
        }
        if (!"1.0".equals(normalize(request.schemaVersion()))) {
            throw new BusinessException(
                    "PAYMENT_SCHEMA_UNSUPPORTED",
                    "Unsupported Payment result schema version",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.paymentId() == null && blankToNull(request.paymentPublicId()) == null) {
            throw new BusinessException(
                    "PAYMENT_IDENTITY_MISSING",
                    "Payment result must contain paymentPublicId or legacy paymentId",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessException(
                    "PAYMENT_AMOUNT_INVALID",
                    "Payment result amount must be positive",
                    HttpStatus.BAD_REQUEST);
        }
        if (normalizeCurrency(request.currency()).isBlank()) {
            throw new BusinessException(
                    "PAYMENT_CURRENCY_INVALID",
                    "Payment result currency is required",
                    HttpStatus.BAD_REQUEST);
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

    private InternalPaymentContextResponse.AnalyticsSnapshot buildAnalyticsSnapshot(
            Booking booking,
            BookingPriceSnapshotPayload priceSnapshot) {
        BookingSnapshot presentationSnapshot = bookingSnapshotRepository == null
                ? null
                : bookingSnapshotRepository.findByBookingId(booking.getId()).orElse(null);

        Long movieId = priceSnapshot.movieId() != null
                ? priceSnapshot.movieId()
                : presentationSnapshot != null && presentationSnapshot.getMovieId() != null
                        ? presentationSnapshot.getMovieId()
                        : booking.getMovieId();
        String movieTitle = blankToNull(priceSnapshot.movieTitle());
        if (movieTitle == null && presentationSnapshot != null) {
            movieTitle = blankToNull(presentationSnapshot.getMovieTitle());
        }
        if (movieTitle == null) {
            movieTitle = "Phim đã đặt";
        }

        int ticketCount = priceSnapshot.seats() == null ? 0 : priceSnapshot.seats().size();
        if (ticketCount <= 0 && presentationSnapshot != null
                && presentationSnapshot.getSeatCount() != null) {
            ticketCount = presentationSnapshot.getSeatCount();
        }
        if (ticketCount <= 0 && reservationRepository != null) {
            ticketCount = reservationRepository.findAllByBookingId(booking.getId()).size();
        }
        if (ticketCount <= 0) {
            throw new BusinessException(
                    "BOOKING_PRESENTATION_SNAPSHOT_INVALID",
                    "Booking ticket count is unavailable",
                    HttpStatus.CONFLICT);
        }

        return new InternalPaymentContextResponse.AnalyticsSnapshot(
                movieId,
                priceSnapshot.moviePublicId(),
                movieTitle,
                booking.getShowtimePublicId(),
                priceSnapshot.cinemaPublicId(),
                ticketCount,
                booking.getTicketAmount(),
                booking.getFoodAmount(),
                defaultAmount(booking.getPromotionDiscount())
                        .add(defaultAmount(booking.getVoucherDiscount()))
                        .add(defaultAmount(booking.getScoreDiscount())),
                booking.getFinalAmount(),
                normalizeCurrency(booking.getCurrency()),
                priceSnapshot.showtimeStartsAt(),
                priceSnapshot.auditoriumPublicId(),
                priceSnapshot.auditoriumCapacity(),
                priceSnapshot.format());
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

    private void confirmReservations(Long bookingId, Instant now) {
        if (reservationRepository == null) {
            return;
        }
        List<SeatReservation> reservations = reservationRepository.findAllByBookingId(bookingId);
        if (reservations.isEmpty() || reservations.stream().anyMatch(reservation ->
                reservation.getStatus() != SeatReservationStatus.HELD
                        || reservation.getExpiresAt() == null
                        || !reservation.getExpiresAt().isAfter(now))) {
            throw new BusinessException(
                    "BOOKING_RESERVATIONS_NOT_HELD",
                    "All Booking seats must remain HELD until payment succeeds",
                    HttpStatus.CONFLICT);
        }
        reservations.forEach(reservation -> reservation.setStatus(SeatReservationStatus.BOOKED));
        reservationRepository.saveAll(reservations);
    }

    private String preferredPaymentLabel(InternalPaymentResultRequest request) {
        String provider = normalizeUpper(request.paymentProvider());
        return provider.isBlank() ? normalizeUpper(request.paymentMethod()) : provider;
    }

    private String paymentIdentity(InternalPaymentResultRequest request) {
        String publicId = blankToNull(request.paymentPublicId());
        return publicId != null ? publicId : String.valueOf(request.paymentId());
    }

    private String reference(InternalPaymentResultRequest request) {
        String transactionCode = blankToNull(request.paymentTransactionCode());
        if (transactionCode != null) {
            return transactionCode;
        }
        String paymentPublicId = blankToNull(request.paymentPublicId());
        return paymentPublicId != null ? paymentPublicId : String.valueOf(request.paymentId());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "PAYMENT_RESULT_INVALID",
                    "Cannot serialize normalized Payment result",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private String normalizeCurrency(String currency) {
        return normalizeUpper(currency);
    }

    private String normalizeUpper(String value) {
        return normalize(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeLower(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    private String stableScoreKey(String purpose, String source) {
        return purpose + "-" + UUID.nameUUIDFromBytes(
                (purpose + ":" + source).getBytes(StandardCharsets.UTF_8));
    }
}
