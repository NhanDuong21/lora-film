package com.project.paymentservice.service;

import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.entity.CashPaymentDetail;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentReconciliationCase;
import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.PaymentLogEventType;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.provider.ProviderSessionUncertainException;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.project.paymentservice.repository.PaymentReconciliationCaseRepository;
import com.project.paymentservice.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentTransactionService {
    private final PaymentRepository paymentRepository;
    private final BookingPaymentGuardRepository guardRepository;
    private final PaymentAnalyticsSnapshotRepository snapshotRepository;
    private final CashPaymentDetailRepository cashRepository;
    private final PaymentReconciliationCaseRepository reconciliationRepository;
    private final PaymentIdempotencyRecordRepository idempotencyRepository;
    private final TransactionCodeGenerator transactionCodeGenerator;
    private final PaymentLogService logService;
    private final PaymentOutboxService outboxService;
    private final PaymentStateTransitionService transitionService;
    private final RefundService refundService;

    public PaymentTransactionService(
            PaymentRepository paymentRepository,
            BookingPaymentGuardRepository guardRepository,
            PaymentAnalyticsSnapshotRepository snapshotRepository,
            CashPaymentDetailRepository cashRepository,
            PaymentReconciliationCaseRepository reconciliationRepository,
            PaymentIdempotencyRecordRepository idempotencyRepository,
            TransactionCodeGenerator transactionCodeGenerator,
            PaymentLogService logService,
            PaymentOutboxService outboxService,
            PaymentStateTransitionService transitionService,
            RefundService refundService) {
        this.paymentRepository = paymentRepository;
        this.guardRepository = guardRepository;
        this.snapshotRepository = snapshotRepository;
        this.cashRepository = cashRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionCodeGenerator = transactionCodeGenerator;
        this.logService = logService;
        this.outboxService = outboxService;
        this.transitionService = transitionService;
        this.refundService = refundService;
    }

    @Transactional
    public Payment reserveAttempt(
            BookingPaymentContext context,
            ProviderCode provider,
            Long actorAccountId,
            Long idempotencyRecordId,
            String idempotencyOwnerToken) {
        String lockedProvider = context.getLockedPaymentProvider();
        if (lockedProvider == null || lockedProvider.isBlank()) {
            throw new BusinessException(
                    "BOOKING_PAYMENT_PROVIDER_NOT_LOCKED",
                    "Booking did not lock an eligible payment provider",
                    HttpStatus.CONFLICT);
        }
        if (!provider.name().equalsIgnoreCase(lockedProvider.trim())) {
            throw new BusinessException(
                    "PAYMENT_PROVIDER_MISMATCH",
                    "Requested provider does not match the provider locked by Booking",
                    HttpStatus.CONFLICT);
        }
        guardRepository.insertIfAbsent(context.getBookingPublicId(), context.getBookingId());
        BookingPaymentGuard guard = guardRepository
                .findByBookingPublicIdForUpdate(context.getBookingPublicId())
                .orElseThrow(() -> new IllegalStateException("Payment guard not found"));
        if (guard.getSuccessfulPaymentId() != null) {
            throw new BusinessException("BOOKING_ALREADY_PAID",
                    "Đơn đặt vé đã được thanh toán", HttpStatus.CONFLICT);
        }
        if (guard.getActivePaymentId() != null) {
            Payment active = paymentRepository.findById(guard.getActivePaymentId()).orElse(null);
            if (active != null && transitionService.isActive(active.getStatus())) {
                throw new BusinessException("PAYMENT_ATTEMPT_ACTIVE",
                        "Đơn đang có một giao dịch thanh toán hoạt động", HttpStatus.CONFLICT);
            }
            guard.setActivePaymentId(null);
        }

        Payment payment = new Payment();
        payment.setPublicId(UUID.randomUUID().toString());
        payment.setPaymentTransactionCode(transactionCodeGenerator.generate(context.getBookingId()));
        payment.setBookingPublicId(context.getBookingPublicId());
        payment.setBookingId(context.getBookingId());
        payment.setAccountId(context.getAccountId());
        payment.setAttemptNumber(guard.getNextAttemptNumber());
        payment.setAmount(context.getAmount());
        payment.setCurrency(context.getCurrency().toUpperCase());
        payment.setBookingAmountLockedAt(context.getAmountLockedAt());
        payment.setBookingExpiresAt(context.getExpiresAt());
        payment.setPaymentMethod(provider == ProviderCode.CASH ? PaymentMethod.CASH : PaymentMethod.ONLINE);
        payment.setProviderCode(provider);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setReconciliationStatus(ReconciliationStatus.NONE);
        payment = paymentRepository.saveAndFlush(payment);

        PaymentAnalyticsSnapshot snapshot = toSnapshot(payment, context);
        snapshotRepository.save(snapshot);

        guard.setActivePaymentId(payment.getId());
        guard.setNextAttemptNumber(guard.getNextAttemptNumber() + 1);
        guardRepository.save(guard);
        com.project.paymentservice.entity.PaymentIdempotencyRecord idempotency =
                idempotencyRepository.findById(idempotencyRecordId)
                        .orElseThrow(() -> new IllegalStateException("Idempotency record missing"));
        if (!idempotencyOwnerToken.equals(idempotency.getLockedBy())) {
            throw new BusinessException("IDEMPOTENCY_OWNER_MISMATCH",
                    "Khóa chống trùng đã được tiến trình khác tiếp quản", HttpStatus.CONFLICT);
        }
        idempotency.setPaymentId(payment.getId());
        idempotencyRepository.save(idempotency);
        logService.log(payment.getId(), PaymentLogEventType.PAYMENT_CREATED,
                "PAYMENT_API", provider == ProviderCode.CASH ? ActorType.EMPLOYEE : ActorType.CUSTOMER,
                actorAccountId, null, PaymentStatus.PENDING,
                "Payment attempt created from authoritative Booking context", "{}");
        return payment;
    }

    @Transactional
    public Payment finalizeProviderSession(
            Long paymentId,
            PaymentSession session,
            Instant nextProviderStatusCheckAt) {
        Payment payment = lock(paymentId);
        if (payment.getStatus() == PaymentStatus.PROCESSING
                && payment.getProviderOrderId() != null) {
            return payment;
        }
        requireStatus(payment, PaymentStatus.PENDING);
        payment.setProviderOrderId(session.getProviderOrderId());
        payment.setProviderSessionId(session.getProviderSessionId());
        payment.setProviderSessionExpiresAt(min(session.getExpiresAt(), payment.getBookingExpiresAt()));
        payment.setLatestProviderSummarySanitized(
                session.getSanitizedProviderSummary() == null ? "{}" : session.getSanitizedProviderSummary());
        payment.setSettlementHoldUntil(nextProviderStatusCheckAt);
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
        logService.log(payment.getId(), PaymentLogEventType.PROVIDER_SESSION_CREATED,
                payment.getProviderCode().name(), ActorType.SYSTEM, null,
                PaymentStatus.PENDING, PaymentStatus.PROCESSING,
                "Provider session created", "{}");
        return payment;
    }

    @Transactional
    public Payment markSessionFailure(Long paymentId, String code, String message) {
        Payment payment = lock(paymentId);
        if (!transitionService.isActive(payment.getStatus())) {
            return payment;
        }
        PaymentStatus previous = payment.getStatus();
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedAt(Instant.now());
        payment.setFailureCode(code);
        payment.setFailureMessageSanitized(sanitize(message, 1000));
        releaseActiveGuard(payment);
        paymentRepository.save(payment);
        logService.log(payment.getId(), PaymentLogEventType.PAYMENT_FAILED,
                "PAYMENT_PROVIDER", ActorType.SYSTEM, null,
                previous, PaymentStatus.FAILED, "Provider session creation failed", "{}");
        return payment;
    }

    @Transactional
    public Payment markSessionUncertain(
            Long paymentId,
            ProviderSessionUncertainException exception,
            Instant settlementHoldUntil) {
        Payment payment = lock(paymentId);
        if (!transitionService.isActive(payment.getStatus())) {
            return payment;
        }
        PaymentStatus previous = payment.getStatus();
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setProviderOrderId(exception.getProviderOrderId());
        payment.setProviderSessionId(exception.getProviderSessionId());
        payment.setSettlementHoldUntil(settlementHoldUntil);
        payment.setFailureCode("PROVIDER_RESULT_UNCERTAIN");
        payment.setFailureMessageSanitized(sanitize(exception.getMessage(), 1000));
        payment.setLatestProviderSummarySanitized(
                exception.getSanitizedSummary() == null ? "{}" : exception.getSanitizedSummary());
        paymentRepository.save(payment);
        logService.log(payment.getId(), PaymentLogEventType.PROVIDER_SESSION_CREATED,
                payment.getProviderCode().name(), ActorType.SYSTEM, null,
                previous, PaymentStatus.PROCESSING,
                "Provider response uncertain; status query scheduled", "{}");
        return payment;
    }

    @Transactional
    public void deferUncertainStatus(Long paymentId, Instant nextCheckAt) {
        Payment payment = lock(paymentId);
        if ((payment.getStatus() == PaymentStatus.PROCESSING
                || payment.getStatus() == PaymentStatus.EXPIRED)
                && payment.getSettlementHoldUntil() != null) {
            payment.setSettlementHoldUntil(nextCheckAt);
            paymentRepository.save(payment);
        }
    }

    @Transactional
    public void scheduleProviderStatusCheck(Long paymentId, Instant nextCheckAt) {
        Payment payment = lock(paymentId);
        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            payment.setSettlementHoldUntil(nextCheckAt);
            paymentRepository.save(payment);
        }
    }

    @Transactional
    public Payment expireAttempt(Long paymentId, Instant now) {
        Payment payment = lock(paymentId);
        if (!transitionService.isActive(payment.getStatus())
                || payment.getBookingExpiresAt().isAfter(now)) {
            return payment;
        }
        PaymentStatus previous = payment.getStatus();
        payment.setStatus(PaymentStatus.EXPIRED);
        payment.setExpiredAt(now);
        payment.setFailureCode("BOOKING_PAYMENT_DEADLINE_EXPIRED");
        payment.setFailureMessageSanitized("Original Booking payment deadline expired");
        releaseActiveGuard(payment);
        paymentRepository.save(payment);
        outboxService.enqueueBookingResult(payment, "TIMEOUT", now);
        logService.log(payment.getId(), PaymentLogEventType.PAYMENT_EXPIRED,
                "PAYMENT_EXPIRY_SCHEDULER", ActorType.SYSTEM, null,
                previous, PaymentStatus.EXPIRED,
                "Original Booking payment deadline expired", "{}");
        return payment;
    }

    @Transactional
    public Payment cancelBeforeProviderSession(String paymentPublicId, Long accountId) {
        Payment payment = paymentRepository.findByPublicIdForUpdate(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId));
        requireOwner(payment, accountId);
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return payment;
        }
        if (payment.getProviderSessionId() != null || payment.getStatus() == PaymentStatus.PROCESSING) {
            throw new BusinessException("PAYMENT_PROVIDER_SESSION_ACTIVE",
                    "Phiên thanh toán đang hoạt động; vui lòng tiếp tục hoặc chờ kết quả",
                    HttpStatus.CONFLICT);
        }
        requireStatus(payment, PaymentStatus.PENDING);
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(Instant.now());
        releaseActiveGuard(payment);
        paymentRepository.save(payment);
        outboxService.enqueueBookingResult(payment, "CANCELLED", payment.getCancelledAt());
        logService.log(payment.getId(), PaymentLogEventType.PAYMENT_CANCELLED,
                "PAYMENT_API", ActorType.CUSTOMER, accountId,
                PaymentStatus.PENDING, PaymentStatus.CANCELLED,
                "Customer cancelled payment before provider session activation", "{}");
        return payment;
    }

    @Transactional
    public Payment collectCash(
            String paymentPublicId,
            Long employeeId,
            BigDecimal receivedAmount,
            String note,
            BookingPaymentContext freshContext) {
        Payment payment = paymentRepository.findByPublicIdForUpdate(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId));
        if (payment.getProviderCode() != ProviderCode.CASH) {
            throw new BusinessException("PAYMENT_NOT_CASH",
                    "Giao dịch không phải thanh toán tiền mặt", HttpStatus.CONFLICT);
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return payment;
        }
        requireStatus(payment, PaymentStatus.PENDING);
        Instant now = Instant.now();
        if (!payment.getBookingExpiresAt().isAfter(now)
                || freshContext == null
                || !Boolean.TRUE.equals(freshContext.getPayable())
                || freshContext.getAmount().compareTo(payment.getAmount()) != 0) {
            throw new BusinessException("BOOKING_NOT_PAYABLE",
                    "Đơn đã hết hạn hoặc số tiền đã thay đổi", HttpStatus.CONFLICT);
        }
        if (receivedAmount == null || receivedAmount.compareTo(payment.getAmount()) < 0) {
            throw new BusinessException("CASH_AMOUNT_INSUFFICIENT",
                    "Số tiền khách đưa chưa đủ", HttpStatus.BAD_REQUEST);
        }
        CashPaymentDetail detail = new CashPaymentDetail();
        detail.setPayment(payment);
        detail.setReceivedAmount(receivedAmount);
        detail.setChangeAmount(receivedAmount.subtract(payment.getAmount()));
        detail.setCollectedByAccountId(employeeId);
        detail.setCollectedAt(now);
        detail.setNoteSanitized(sanitize(note, 500));
        cashRepository.save(detail);

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setSucceededAt(now);
        payment.setExternalTransactionId("CASH-" + payment.getPaymentTransactionCode());
        markSuccessfulGuard(payment);
        paymentRepository.save(payment);
        outboxService.enqueueBookingResult(payment, "SUCCESS", now);
        logService.log(payment.getId(), PaymentLogEventType.CASH_PAYMENT_COLLECTED,
                "CASH_COUNTER", ActorType.EMPLOYEE, employeeId,
                PaymentStatus.PENDING, PaymentStatus.SUCCESS,
                "Cash collected at counter", "{}");
        return payment;
    }

    @Transactional
    public Payment cancelCash(String paymentPublicId, Long employeeId, String reason) {
        Payment payment = paymentRepository.findByPublicIdForUpdate(paymentPublicId)
                .orElseThrow(() -> notFound(paymentPublicId));
        if (payment.getProviderCode() != ProviderCode.CASH) {
            throw new BusinessException("PAYMENT_NOT_CASH",
                    "Giao dịch không phải thanh toán tiền mặt", HttpStatus.CONFLICT);
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return payment;
        }
        requireStatus(payment, PaymentStatus.PENDING);
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(Instant.now());
        payment.setFailureCode("CASH_CANCELLED");
        payment.setFailureMessageSanitized(sanitize(reason, 500));
        releaseActiveGuard(payment);
        paymentRepository.save(payment);
        outboxService.enqueueBookingResult(payment, "CANCELLED", payment.getCancelledAt());
        logService.log(payment.getId(), PaymentLogEventType.PAYMENT_CANCELLED,
                "CASH_COUNTER", ActorType.EMPLOYEE, employeeId,
                PaymentStatus.PENDING, PaymentStatus.CANCELLED,
                "Cash payment cancelled at counter", "{}");
        return payment;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public Payment applyProviderResult(
            ProviderCode provider,
            ProviderCallbackResult result,
            Long webhookEventId) {
        Payment found = paymentRepository
                .findByProviderCodeAndProviderOrderId(provider, result.getProviderOrderId())
                .orElseThrow(() -> new BusinessException(
                        "PAYMENT_ORDER_NOT_FOUND",
                        "Không tìm thấy giao dịch của provider",
                        HttpStatus.NOT_FOUND));
        Payment payment = lock(found.getId());
        if (result.getAmount() == null
                || result.getAmount().compareTo(payment.getAmount()) != 0
                || result.getCurrency() == null
                || !result.getCurrency().equalsIgnoreCase(payment.getCurrency())) {
            requireReconciliation(payment, webhookEventId, "PROVIDER_AMOUNT_MISMATCH",
                    result.getDeduplicationKey(),
                    "Provider callback amount/currency differs from locked Payment");
            paymentRepository.save(payment);
            throw new BusinessException("PAYMENT_AMOUNT_MISMATCH",
                    "Số tiền callback không khớp giao dịch", HttpStatus.CONFLICT);
        }

        PaymentStatus previous = payment.getStatus();
        if ("SUCCESS".equals(result.getResult())) {
            if (previous == PaymentStatus.SUCCESS) {
                return payment;
            }
            boolean late = !Instant.now().isBefore(payment.getBookingExpiresAt())
                    || previous == PaymentStatus.CANCELLED
                    || previous == PaymentStatus.EXPIRED;
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setSucceededAt(result.getOccurredAt() == null ? Instant.now() : result.getOccurredAt());
            payment.setExternalTransactionId(result.getExternalTransactionId());
            payment.setProviderResponseCode(result.getResponseCode());
            if (late) {
                requireReconciliation(payment, webhookEventId, "LATE_PROVIDER_SUCCESS",
                        result.getDeduplicationKey(),
                        "Provider reported success after the original Booking deadline");
            }
            BookingPaymentGuard guard = lockGuard(payment);
            boolean duplicateFinancialSuccess = guard.getSuccessfulPaymentId() != null
                    && !guard.getSuccessfulPaymentId().equals(payment.getId());
            if (duplicateFinancialSuccess) {
                requireReconciliation(payment, webhookEventId, "DUPLICATE_FINANCIAL_SUCCESS",
                        result.getDeduplicationKey(),
                        "Another Payment was already successful for this Booking");
            } else {
                guard.setSuccessfulPaymentId(payment.getId());
                guard.setActivePaymentId(null);
                guardRepository.save(guard);
            }
            paymentRepository.save(payment);
            outboxService.enqueueBookingResult(payment, "SUCCESS", payment.getSucceededAt());
            if (late) {
                refundService.createAutomaticFullRefund(
                        payment.getId(),
                        "automatic:late-provider-success:" + payment.getPublicId(),
                        "LATE_PROVIDER_SUCCESS",
                        "Nhà cung cấp ghi nhận thành công sau hạn thanh toán gốc của đơn");
            } else if (duplicateFinancialSuccess) {
                refundService.createAutomaticFullRefund(
                        payment.getId(),
                        "automatic:duplicate-capture:" + payment.getPublicId(),
                        "DUPLICATE_CAPTURE",
                        "Một giao dịch khác của cùng đơn đã thanh toán thành công");
            }
            logService.log(payment.getId(), late
                            ? PaymentLogEventType.LATE_SUCCESS_DETECTED
                            : PaymentLogEventType.PAYMENT_SUCCEEDED,
                    provider.name(), ActorType.PROVIDER, null,
                    previous, PaymentStatus.SUCCESS,
                    late ? "Late provider success requires reconciliation" : "Provider success accepted",
                    "{}");
            return payment;
        }

        if (previous == PaymentStatus.SUCCESS) {
            return payment;
        }
        if (!transitionService.isActive(previous)) {
            payment.setProviderResponseCode(result.getResponseCode());
            payment.setExternalTransactionId(result.getExternalTransactionId());
            payment.setSettlementHoldUntil(null);
            paymentRepository.save(payment);
            return payment;
        }
        PaymentStatus terminal = "CANCELLED".equals(result.getResult())
                ? PaymentStatus.CANCELLED : PaymentStatus.FAILED;
        payment.setStatus(terminal);
        payment.setProviderResponseCode(result.getResponseCode());
        payment.setExternalTransactionId(result.getExternalTransactionId());
        if (terminal == PaymentStatus.CANCELLED) {
            payment.setCancelledAt(Instant.now());
        } else {
            payment.setFailedAt(Instant.now());
        }
        releaseActiveGuard(payment);
        paymentRepository.save(payment);
        outboxService.enqueueBookingResult(payment, terminal.name(), Instant.now());
        return payment;
    }

    @Transactional
    public void recordWebhookPayloadConflict(
            Long paymentId,
            Long webhookEventId,
            String sourceReference,
            String detail) {
        if (paymentId == null) {
            return;
        }
        Payment payment = lock(paymentId);
        requireReconciliation(
                payment,
                webhookEventId,
                "PROVIDER_EVENT_PAYLOAD_CONFLICT",
                sourceReference,
                detail);
        paymentRepository.save(payment);
    }

    private PaymentAnalyticsSnapshot toSnapshot(
            Payment payment, BookingPaymentContext context) {
        BookingPaymentContext.AnalyticsSnapshotData data = context.getAnalyticsSnapshot();
        PaymentAnalyticsSnapshot snapshot = new PaymentAnalyticsSnapshot();
        snapshot.setPayment(payment);
        snapshot.setMovieId(data.getMovieId());
        snapshot.setMoviePublicId(data.getMoviePublicId());
        snapshot.setMovieTitle(data.getMovieTitle());
        snapshot.setShowtimePublicId(data.getShowtimePublicId());
        snapshot.setCinemaPublicId(data.getCinemaPublicId());
        snapshot.setTicketCount(data.getTicketCount());
        snapshot.setTicketAmount(defaultAmount(data.getTicketAmount(), context.getAmount()));
        snapshot.setFoodAmount(defaultAmount(data.getFoodAmount(), BigDecimal.ZERO));
        snapshot.setDiscountAmount(defaultAmount(data.getDiscountAmount(), BigDecimal.ZERO));
        snapshot.setTotalAmount(defaultAmount(data.getTotalAmount(), context.getAmount()));
        snapshot.setCurrency(data.getCurrency() == null ? context.getCurrency() : data.getCurrency());
        return snapshot;
    }

    private void requireReconciliation(Payment payment, Long webhookEventId,
            String reason, String sourceReference, String detail) {
        payment.setReconciliationStatus(ReconciliationStatus.REQUIRED);
        payment.setReconciliationReason(reason);
        if (reconciliationRepository.findByPaymentIdAndReasonCodeAndSourceReference(
                payment.getId(), reason, sourceReference).isPresent()) {
            return;
        }
        PaymentReconciliationCase item = new PaymentReconciliationCase();
        item.setPublicId(UUID.randomUUID().toString());
        item.setPaymentId(payment.getId());
        item.setWebhookEventId(webhookEventId);
        item.setReasonCode(reason);
        item.setSourceReference(sourceReference);
        item.setDetailSanitized(sanitize(detail, 2000));
        reconciliationRepository.save(item);
    }

    private void markSuccessfulGuard(Payment payment) {
        BookingPaymentGuard guard = lockGuard(payment);
        if (guard.getSuccessfulPaymentId() != null
                && !guard.getSuccessfulPaymentId().equals(payment.getId())) {
            throw new BusinessException("BOOKING_ALREADY_PAID",
                    "Đơn đã có giao dịch thành công", HttpStatus.CONFLICT);
        }
        guard.setSuccessfulPaymentId(payment.getId());
        guard.setActivePaymentId(null);
        guardRepository.save(guard);
    }

    private void releaseActiveGuard(Payment payment) {
        BookingPaymentGuard guard = lockGuard(payment);
        if (payment.getId().equals(guard.getActivePaymentId())) {
            guard.setActivePaymentId(null);
            guardRepository.save(guard);
        }
    }

    private BookingPaymentGuard lockGuard(Payment payment) {
        return guardRepository.findByBookingPublicIdForUpdate(payment.getBookingPublicId())
                .orElseThrow(() -> new IllegalStateException("Payment guard missing"));
    }

    private Payment lock(Long paymentId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> notFound(String.valueOf(paymentId)));
    }

    private void requireOwner(Payment payment, Long accountId) {
        if (!payment.getAccountId().equals(accountId)) {
            throw new BusinessException("PAYMENT_ACCESS_DENIED",
                    "Bạn không có quyền truy cập giao dịch này", HttpStatus.FORBIDDEN);
        }
    }

    private void requireStatus(Payment payment, PaymentStatus status) {
        if (payment.getStatus() != status) {
            throw new BusinessException("PAYMENT_INVALID_STATE",
                    "Trạng thái giao dịch không cho phép thao tác này",
                    HttpStatus.CONFLICT);
        }
    }

    private BusinessException notFound(String identity) {
        return new BusinessException("PAYMENT_NOT_FOUND",
                "Không tìm thấy giao dịch: " + identity, HttpStatus.NOT_FOUND);
    }

    private BigDecimal defaultAmount(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private Instant min(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }
}
