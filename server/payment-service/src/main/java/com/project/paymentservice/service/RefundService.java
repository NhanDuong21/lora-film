package com.project.paymentservice.service;

import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.dto.request.CreateRefundRequest;
import com.project.paymentservice.dto.response.RefundResponse;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.enumtype.ActorType;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.RefundComponent;
import com.project.paymentservice.enumtype.RefundStatus;
import com.project.paymentservice.enumtype.RefundType;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.provider.ProviderRefundResult;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentRefundRepository;
import com.project.paymentservice.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RefundService {
    private static final EnumSet<RefundStatus> RESERVED_STATUSES = EnumSet.of(
            RefundStatus.PENDING_APPROVAL,
            RefundStatus.REQUESTED,
            RefundStatus.PROCESSING,
            RefundStatus.SUCCESS,
            RefundStatus.REQUIRES_ACTION);

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentAnalyticsSnapshotRepository snapshotRepository;
    private final PaymentOutboxService outboxService;
    private final PaymentRuntimeProperties properties;

    public RefundService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository refundRepository,
            PaymentAnalyticsSnapshotRepository snapshotRepository,
            PaymentOutboxService outboxService,
            PaymentRuntimeProperties properties) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.snapshotRepository = snapshotRepository;
        this.outboxService = outboxService;
        this.properties = properties;
    }

    @Transactional
    public RefundResponse createAdminRefund(
            String paymentPublicId,
            String idempotencyKey,
            Long adminAccountId,
            CreateRefundRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(
                    "REFUND_IDEMPOTENCY_KEY_REQUIRED",
                    "Yêu cầu hoàn tiền phải có khóa chống trùng",
                    HttpStatus.BAD_REQUEST);
        }
        Payment payment = paymentRepository.findByPublicIdForUpdate(paymentPublicId)
                .orElseThrow(() -> paymentNotFound(paymentPublicId));
        PaymentRefund existing = refundRepository
                .findByPaymentIdAndRequestKey(payment.getId(), normalizeKey(idempotencyKey))
                .orElse(null);
        if (existing != null) {
            verifyReplay(existing, request);
            return RefundResponse.from(existing);
        }
        validateRefundablePayment(payment);
        validateAdminRequest(request);

        BigDecimal remaining = refundableRemaining(payment);
        BigDecimal amount = request.getRefundType() == RefundType.FULL
                ? remaining : request.getAmount();
        requireAvailableAmount(amount, remaining);
        if (request.getRefundComponent() == RefundComponent.CONCESSION) {
            PaymentAnalyticsSnapshot snapshot = snapshotRepository
                    .findByPaymentId(payment.getId())
                    .orElseThrow(() -> new BusinessException(
                            "PAYMENT_SNAPSHOT_MISSING",
                            "Không tìm thấy dữ liệu bắp nước của đơn",
                            HttpStatus.CONFLICT));
            BigDecimal alreadyReserved = refundRepository.sumReservedAmountByComponent(
                    payment.getId(), RefundComponent.CONCESSION, RESERVED_STATUSES);
            BigDecimal concessionRemaining = snapshot.getFoodAmount().subtract(alreadyReserved);
            requireAvailableAmount(amount, concessionRemaining);
        }

        RefundType providerOperation = request.getRefundType() == RefundType.FULL
                && amount.compareTo(payment.getAmount()) < 0
                ? RefundType.PARTIAL
                : request.getRefundType();
        PaymentRefund refund = newRefund(
                payment,
                normalizeKey(idempotencyKey),
                providerOperation,
                request.getRefundComponent(),
                normalizeCode(request.getReasonCode()),
                sanitize(request.getNote(), 2000),
                amount,
                false,
                ActorType.ADMIN,
                adminAccountId,
                false);
        return RefundResponse.from(refundRepository.saveAndFlush(refund));
    }

    @Transactional
    public RefundResponse createAccountingRefundRequest(
            String paymentPublicId,
            String idempotencyKey,
            Long accountingAccountId,
            String cinemaPublicId,
            CreateRefundRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(
                    "REFUND_IDEMPOTENCY_KEY_REQUIRED",
                    "Yêu cầu hoàn tiền phải có mã chống xử lý trùng.",
                    HttpStatus.BAD_REQUEST);
        }
        Payment payment = paymentRepository.findByPublicIdForUpdate(paymentPublicId)
                .orElseThrow(() -> paymentNotFound(paymentPublicId));
        if (cinemaPublicId != null && !cinemaPublicId.isBlank()) {
            requirePaymentCinema(payment, cinemaPublicId);
        }
        PaymentRefund existing = refundRepository
                .findByPaymentIdAndRequestKey(payment.getId(), normalizeKey(idempotencyKey))
                .orElse(null);
        if (existing != null) {
            verifyReplay(existing, request);
            return RefundResponse.from(existing);
        }
        validateRefundablePayment(payment);
        validateAdminRequest(request);
        BigDecimal remaining = refundableRemaining(payment);
        BigDecimal amount = request.getRefundType() == RefundType.FULL
                ? remaining : request.getAmount();
        requireAvailableAmount(amount, remaining);
        if (request.getRefundComponent() == RefundComponent.CONCESSION) {
            PaymentAnalyticsSnapshot snapshot = snapshotRepository.findByPaymentId(payment.getId())
                    .orElseThrow(() -> new BusinessException(
                            "PAYMENT_SNAPSHOT_MISSING",
                            "Không tìm thấy dữ liệu bắp nước của đơn.",
                            HttpStatus.CONFLICT));
            BigDecimal reserved = refundRepository.sumReservedAmountByComponent(
                    payment.getId(), RefundComponent.CONCESSION, RESERVED_STATUSES);
            requireAvailableAmount(amount, snapshot.getFoodAmount().subtract(reserved));
        }
        RefundType providerOperation = request.getRefundType() == RefundType.FULL
                && amount.compareTo(payment.getAmount()) < 0
                ? RefundType.PARTIAL : request.getRefundType();
        PaymentRefund refund = newRefund(
                payment, normalizeKey(idempotencyKey), providerOperation,
                request.getRefundComponent(), normalizeCode(request.getReasonCode()),
                sanitize(request.getNote(), 2000), amount, false,
                ActorType.EMPLOYEE, accountingAccountId, true);
        return RefundResponse.from(refundRepository.saveAndFlush(refund));
    }

    @Transactional
    public RefundResponse createEmployeeRefundRequest(
            String paymentPublicId,
            String idempotencyKey,
            Long employeeAccountId,
            String employeeCinemaPublicId,
            CreateRefundRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(
                    "REFUND_IDEMPOTENCY_KEY_REQUIRED",
                    "Yêu cầu hoàn tiền phải có khóa chống trùng",
                    HttpStatus.BAD_REQUEST);
        }
        Payment payment = paymentRepository.findByPublicIdForUpdate(paymentPublicId)
                .orElseThrow(() -> paymentNotFound(paymentPublicId));
        requirePaymentCinema(payment, employeeCinemaPublicId);
        PaymentRefund existing = refundRepository
                .findByPaymentIdAndRequestKey(payment.getId(), normalizeKey(idempotencyKey))
                .orElse(null);
        if (existing != null) {
            verifyReplay(existing, request);
            return RefundResponse.from(existing);
        }
        validateRefundablePayment(payment);
        validateAdminRequest(request);

        BigDecimal remaining = refundableRemaining(payment);
        BigDecimal amount = request.getRefundType() == RefundType.FULL
                ? remaining : request.getAmount();
        requireAvailableAmount(amount, remaining);
        if (request.getRefundType() == RefundType.PARTIAL
                && amount.compareTo(remaining) >= 0) {
            throw new BusinessException(
                    "REFUND_PARTIAL_MUST_BE_LESS_THAN_REMAINING",
                    "Hoàn một phần phải nhỏ hơn số tiền còn có thể hoàn. Hãy chọn hoàn toàn bộ nếu cần hoàn hết.",
                    HttpStatus.BAD_REQUEST);
        }
        if (request.getRefundComponent() == RefundComponent.CONCESSION) {
            PaymentAnalyticsSnapshot snapshot = snapshotRepository
                    .findByPaymentId(payment.getId())
                    .orElseThrow(() -> new BusinessException(
                            "PAYMENT_SNAPSHOT_MISSING",
                            "Không tìm thấy dữ liệu bắp nước của đơn",
                            HttpStatus.CONFLICT));
            BigDecimal alreadyReserved = refundRepository.sumReservedAmountByComponent(
                    payment.getId(), RefundComponent.CONCESSION, RESERVED_STATUSES);
            requireAvailableAmount(amount, snapshot.getFoodAmount().subtract(alreadyReserved));
        }
        RefundType providerOperation = request.getRefundType() == RefundType.FULL
                && amount.compareTo(payment.getAmount()) < 0
                ? RefundType.PARTIAL : request.getRefundType();
        PaymentRefund refund = newRefund(
                payment,
                normalizeKey(idempotencyKey),
                providerOperation,
                request.getRefundComponent(),
                normalizeCode(request.getReasonCode()),
                sanitize(request.getNote(), 2000),
                amount,
                false,
                ActorType.EMPLOYEE,
                employeeAccountId,
                true);
        return RefundResponse.from(refundRepository.saveAndFlush(refund));
    }

    @Transactional
    public PaymentRefund createAutomaticFullRefund(
            Long paymentId,
            String requestKey,
            String reasonCode,
            String detail) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> paymentNotFound(String.valueOf(paymentId)));
        PaymentRefund existing = refundRepository
                .findByPaymentIdAndRequestKey(payment.getId(), normalizeKey(requestKey))
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        validateRefundablePayment(payment);
        BigDecimal remaining = refundableRemaining(payment);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        RefundType providerOperation = remaining.compareTo(payment.getAmount()) == 0
                ? RefundType.FULL : RefundType.PARTIAL;
        PaymentRefund refund = newRefund(
                payment,
                normalizeKey(requestKey),
                providerOperation,
                RefundComponent.FULL_ORDER,
                normalizeCode(reasonCode),
                sanitize(detail, 2000),
                remaining,
                true,
                ActorType.SYSTEM,
                null,
                false);
        return refundRepository.saveAndFlush(refund);
    }

    @Transactional
    public int createShowtimeCancellationRefunds(
            String showtimePublicId,
            String eventId,
            String note) {
        int created = 0;
        for (Long paymentId : snapshotRepository
                .findSuccessfulPaymentIdsByShowtimePublicId(showtimePublicId)) {
            PaymentRefund refund = createAutomaticFullRefund(
                    paymentId,
                    "showtime-cancelled:" + eventId,
                    "SHOWTIME_CANCELLED",
                    note == null || note.isBlank()
                            ? "Suất chiếu đã bị hủy" : note);
            if (refund != null) {
                created++;
            }
        }
        return created;
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> list(RefundStatus status, Pageable pageable) {
        Page<PaymentRefund> page = status == null
                ? refundRepository.findAll(pageable)
                : refundRepository.findByStatus(status, pageable);
        page.getContent().forEach(value -> value.getPayment().getPublicId());
        return page.map(RefundResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> listForCinema(
            String cinemaPublicId, RefundStatus status, Pageable pageable) {
        List<Long> paymentIds = snapshotRepository.findPaymentIdsByCinemaPublicId(cinemaPublicId);
        if (paymentIds.isEmpty()) return Page.empty(pageable);
        Page<PaymentRefund> page = status == null
                ? refundRepository.findByPaymentIdIn(paymentIds, pageable)
                : refundRepository.findByPaymentIdInAndStatus(paymentIds, status, pageable);
        page.getContent().forEach(value -> value.getPayment().getPublicId());
        return page.map(RefundResponse::from);
    }

    @Transactional
    public RefundResponse approve(
            String refundPublicId,
            String cinemaPublicId,
            Long managerAccountId,
            String note) {
        PaymentRefund refund = refundForDecision(refundPublicId, cinemaPublicId);
        requireIndependentReviewer(refund, managerAccountId);
        Instant now = Instant.now();
        refund.setReviewedByAccountId(managerAccountId);
        refund.setReviewedAt(now);
        refund.setReviewNoteSanitized(sanitize(note, 1000));
        if (refund.getProviderCode() == ProviderCode.CASH) {
            refund.setStatus(RefundStatus.REQUIRES_ACTION);
            refund.setFailureCode("CASH_REFUND_REQUIRES_MANUAL_SETTLEMENT");
            refund.setFailureMessageSanitized(
                    "Hoàn tiền mặt cần được nhân viên xử lý tại quầy");
            refund.setNextAttemptAt(null);
        } else {
            refund.setStatus(RefundStatus.REQUESTED);
            refund.setNextAttemptAt(now);
        }
        return RefundResponse.from(refundRepository.save(refund));
    }

    @Transactional
    public RefundResponse reject(
            String refundPublicId,
            String cinemaPublicId,
            Long managerAccountId,
            String note) {
        PaymentRefund refund = refundForDecision(refundPublicId, cinemaPublicId);
        requireIndependentReviewer(refund, managerAccountId);
        refund.setStatus(RefundStatus.REJECTED);
        refund.setReviewedByAccountId(managerAccountId);
        refund.setReviewedAt(Instant.now());
        refund.setReviewNoteSanitized(sanitize(note, 1000));
        refund.setNextAttemptAt(null);
        return RefundResponse.from(refundRepository.save(refund));
    }

    @Transactional(readOnly = true)
    public RefundResponse detail(String refundPublicId) {
        PaymentRefund refund = refundRepository.findByPublicId(refundPublicId)
                .orElseThrow(() -> refundNotFound(refundPublicId));
        refund.getPayment().getPublicId();
        return RefundResponse.from(refund);
    }

    @Transactional
    public RefundResponse retry(String refundPublicId) {
        PaymentRefund refund = refundRepository.findByPublicId(refundPublicId)
                .orElseThrow(() -> refundNotFound(refundPublicId));
        refund = refundRepository.findByIdForUpdate(refund.getId())
                .orElseThrow(() -> refundNotFound(refundPublicId));
        if (refund.getStatus() != RefundStatus.FAILED
                && refund.getStatus() != RefundStatus.REQUIRES_ACTION) {
            throw new BusinessException(
                    "REFUND_NOT_RETRYABLE",
                    "Yêu cầu hoàn tiền này không ở trạng thái có thể thử lại",
                    HttpStatus.CONFLICT);
        }
        if (refund.getProviderCode() == ProviderCode.CASH) {
            throw new BusinessException(
                    "CASH_REFUND_REQUIRES_MANUAL_SETTLEMENT",
                    "Hoàn tiền mặt cần được xử lý và ghi nhận tại quầy",
                    HttpStatus.CONFLICT);
        }
        boolean providerMayAlreadyHaveTheRequest =
                refund.getStatus() == RefundStatus.REQUIRES_ACTION
                && refund.getSubmittedAt() != null;
        refund.setStatus(providerMayAlreadyHaveTheRequest
                ? RefundStatus.PROCESSING
                : RefundStatus.REQUESTED);
        refund.setRetryCount(0);
        refund.setNextAttemptAt(Instant.now());
        refund.setFailureCode(null);
        refund.setFailureMessageSanitized(null);
        refund.setFailedAt(null);
        return RefundResponse.from(refundRepository.save(refund));
    }

    @Transactional
    public RefundResponse completeCashRefund(
            String refundPublicId,
            Long actorAccountId,
            String providerReference,
            String note) {
        PaymentRefund refund = refundRepository.findByPublicId(refundPublicId)
                .orElseThrow(() -> refundNotFound(refundPublicId));
        refund = refundRepository.findByIdForUpdate(refund.getId())
                .orElseThrow(() -> refundNotFound(refundPublicId));
        if (refund.getProviderCode() != ProviderCode.CASH) {
            throw new BusinessException(
                    "REFUND_NOT_CASH",
                    "Chỉ yêu cầu hoàn tiền mặt mới được xác nhận tại quầy",
                    HttpStatus.CONFLICT);
        }
        if (refund.getStatus() == RefundStatus.SUCCESS) {
            return RefundResponse.from(refund);
        }
        if (refund.getStatus() != RefundStatus.REQUIRES_ACTION) {
            throw new BusinessException(
                    "CASH_REFUND_INVALID_STATE",
                    "Yêu cầu hoàn tiền mặt chưa ở trạng thái có thể xác nhận",
                    HttpStatus.CONFLICT);
        }
        String reference = sanitize(providerReference, 150);
        if (reference == null || reference.isBlank()) {
            throw new BusinessException(
                    "CASH_REFUND_REFERENCE_REQUIRED",
                    "Vui lòng nhập mã biên nhận hoàn tiền tại quầy",
                    HttpStatus.BAD_REQUEST);
        }
        String auditNote = sanitize(note, 1000);
        if (auditNote == null || auditNote.isBlank()) {
            throw new BusinessException(
                    "CASH_REFUND_NOTE_REQUIRED",
                    "Vui lòng ghi chú cách thức đã trả tiền cho khách",
                    HttpStatus.BAD_REQUEST);
        }
        Instant now = Instant.now();
        refund.setProviderRefundId(reference);
        refund.setProviderResponseCode("MANUAL_CASH_SETTLED");
        refund.setCompletedByAccountId(actorAccountId);
        refund.setProviderSummarySanitized(
                "{\"settledByAccountId\":" + actorAccountId + "}");
        refund.setReasonDetailSanitized(sanitize(
                firstNonBlank(refund.getReasonDetailSanitized(), "")
                        + " | Xác nhận tại quầy: " + auditNote,
                2000));
        refund.setStatus(RefundStatus.SUCCESS);
        refund.setSucceededAt(now);
        refund.setFailureCode(null);
        refund.setFailureMessageSanitized(null);
        refund.setNextAttemptAt(null);
        clearLease(refund);
        refundRepository.save(refund);
        outboxService.enqueueBookingRefundResult(refund, true, now);
        return RefundResponse.from(refund);
    }

    @Transactional
    public RefundResponse completeEmployeeCashRefund(
            String refundPublicId,
            Long employeeAccountId,
            String employeeCinemaPublicId,
            String providerReference,
            String note) {
        PaymentRefund refund = refundRepository.findByPublicId(refundPublicId)
                .orElseThrow(() -> refundNotFound(refundPublicId));
        requirePaymentCinema(refund.getPayment(), employeeCinemaPublicId);
        return completeCashRefund(
                refundPublicId, employeeAccountId, providerReference, note);
    }

    @Transactional
    public List<Long> claimReady(String ownerToken) {
        Instant now = Instant.now();
        List<PaymentRefund> candidates = refundRepository.findReady(
                EnumSet.of(RefundStatus.REQUESTED, RefundStatus.PROCESSING),
                now,
                PageRequest.of(0, properties.getRefundBatchSize()));
        Instant lockedUntil = now.plusSeconds(properties.getRefundLeaseSeconds());
        return candidates.stream().map(candidate -> {
            PaymentRefund refund = refundRepository.findByIdForUpdate(candidate.getId())
                    .orElseThrow();
            if (refund.getLockedUntil() != null && refund.getLockedUntil().isAfter(now)) {
                return null;
            }
            refund.setLockedBy(ownerToken);
            refund.setLockedAt(now);
            refund.setLockedUntil(lockedUntil);
            refundRepository.save(refund);
            return refund.getId();
        }).filter(java.util.Objects::nonNull).toList();
    }

    @Transactional
    public RefundWork loadOwnedWork(Long refundId, String ownerToken) {
        PaymentRefund refund = owned(refundId, ownerToken);
        Payment payment = paymentRepository.findById(refund.getPayment().getId())
                .orElseThrow(() -> paymentNotFound(String.valueOf(refund.getPayment().getId())));
        refund.setPayment(payment);
        boolean providerRefundKnown = refund.getProviderRefundId() != null
                && !refund.getProviderRefundId().isBlank();
        boolean providerResponseObserved = refund.getProviderResponseCode() != null
                && !refund.getProviderResponseCode().isBlank();
        return new RefundWork(
                payment,
                refund,
                refund.getStatus() == RefundStatus.PROCESSING
                        && (providerRefundKnown || providerResponseObserved));
    }

    @Transactional
    public void markSubmitted(Long refundId, String ownerToken) {
        PaymentRefund refund = owned(refundId, ownerToken);
        if (refund.getStatus() == RefundStatus.REQUESTED) {
            refund.setStatus(RefundStatus.PROCESSING);
            refund.setSubmittedAt(Instant.now());
            refund.setProviderOrderId(refund.getRefundCode());
            refund.setProviderRequestId(refund.getPublicId());
            refundRepository.save(refund);
        }
    }

    @Transactional
    public void applyProviderResult(
            Long refundId,
            String ownerToken,
            ProviderRefundResult result) {
        PaymentRefund refund = owned(refundId, ownerToken);
        refund.setProviderOrderId(firstNonBlank(
                result.getProviderOrderId(), refund.getProviderOrderId()));
        refund.setProviderRequestId(firstNonBlank(
                result.getProviderRequestId(), refund.getProviderRequestId()));
        refund.setProviderRefundId(firstNonBlank(
                result.getProviderRefundId(), refund.getProviderRefundId()));
        refund.setProviderResponseCode(result.getResponseCode());
        refund.setProviderSummarySanitized(
                result.getSummarySanitized() == null ? "{}" : result.getSummarySanitized());
        clearLease(refund);
        if (result.getState() == ProviderRefundResult.State.SUCCESS) {
            if (refund.getStatus() != RefundStatus.SUCCESS) {
                refund.setStatus(RefundStatus.SUCCESS);
                refund.setSucceededAt(result.getOccurredAt() == null
                        ? Instant.now() : result.getOccurredAt());
                refund.setFailureCode(null);
                refund.setFailureMessageSanitized(null);
                refund.setNextAttemptAt(null);
                refundRepository.save(refund);
                outboxService.enqueueBookingRefundResult(
                        refund, true, refund.getSucceededAt());
            }
            return;
        }
        if (result.getState() == ProviderRefundResult.State.PROCESSING) {
            scheduleProviderProcessing(
                    refund,
                    result.getFailureCode(),
                    result.getMessageSanitized(),
                    result.getRetryAfterSeconds());
            return;
        }
        refund.setStatus(RefundStatus.FAILED);
        refund.setFailedAt(Instant.now());
        refund.setFailureCode(firstNonBlank(result.getFailureCode(), "PROVIDER_REFUND_REJECTED"));
        refund.setFailureMessageSanitized(sanitize(result.getMessageSanitized(), 2000));
        refund.setNextAttemptAt(null);
        refundRepository.save(refund);
        outboxService.enqueueBookingRefundResult(refund, false, refund.getFailedAt());
    }

    @Transactional
    public void markUncertain(
            Long refundId,
            String ownerToken,
            String safeMessage) {
        PaymentRefund refund = owned(refundId, ownerToken);
        clearLease(refund);
        scheduleUncertain(refund, "PROVIDER_REFUND_RESULT_UNCERTAIN", safeMessage);
    }

    private void scheduleProviderProcessing(
            PaymentRefund refund,
            String code,
            String message,
            Integer providerRetryAfterSeconds) {
        Instant now = Instant.now();
        Instant processingStartedAt = refund.getSubmittedAt() != null
                ? refund.getSubmittedAt()
                : refund.getRequestedAt();
        refund.setRetryCount(refund.getRetryCount() + 1);
        refund.setFailureCode(firstNonBlank(code, "PROVIDER_REFUND_PROCESSING"));
        refund.setFailureMessageSanitized(sanitize(message, 2000));
        if (processingStartedAt != null
                && !processingStartedAt
                    .plusSeconds(properties.getRefundProcessingMaxAgeHours() * 3600L)
                    .isAfter(now)) {
            refund.setStatus(RefundStatus.REQUIRES_ACTION);
            refund.setNextAttemptAt(null);
        } else {
            refund.setStatus(RefundStatus.PROCESSING);
            long pollSeconds = providerRetryAfterSeconds == null
                    ? properties.getRefundProcessingPollSeconds()
                    : Math.max(
                        properties.getRefundProcessingPollSeconds(),
                        providerRetryAfterSeconds.longValue());
            refund.setNextAttemptAt(now.plusSeconds(
                    pollSeconds));
        }
        refundRepository.save(refund);
    }

    private void scheduleUncertain(
            PaymentRefund refund,
            String code,
            String message) {
        int attempts = refund.getRetryCount() + 1;
        refund.setRetryCount(attempts);
        refund.setFailureCode(firstNonBlank(code, "PROVIDER_REFUND_PROCESSING"));
        refund.setFailureMessageSanitized(sanitize(message, 2000));
        if (attempts >= properties.getRefundMaxAttempts()) {
            refund.setStatus(RefundStatus.REQUIRES_ACTION);
            refund.setNextAttemptAt(null);
        } else {
            refund.setStatus(RefundStatus.PROCESSING);
            long delay = Math.min(300, 1L << Math.min(attempts, 8));
            refund.setNextAttemptAt(Instant.now().plusSeconds(delay));
        }
        refundRepository.save(refund);
    }

    private PaymentRefund newRefund(
            Payment payment,
            String requestKey,
            RefundType type,
            RefundComponent component,
            String reasonCode,
            String detail,
            BigDecimal amount,
            boolean automatic,
            ActorType actor,
            Long actorAccountId,
            boolean approvalRequired) {
        Instant now = Instant.now();
        PaymentRefund refund = new PaymentRefund();
        refund.setPublicId(UUID.randomUUID().toString());
        refund.setRefundCode("RFD-" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        refund.setPayment(payment);
        refund.setRequestKey(requestKey);
        refund.setProviderCode(payment.getProviderCode());
        refund.setRefundType(type);
        refund.setRefundComponent(component);
        refund.setReasonCode(reasonCode);
        refund.setReasonDetailSanitized(detail);
        refund.setRequestedAmount(amount);
        refund.setCurrency(payment.getCurrency());
        refund.setAutomatic(automatic);
        refund.setRequestedByActor(actor);
        refund.setRequestedByAccountId(actorAccountId);
        refund.setRequestedAt(now);
        refund.setRetryCount(0);
        if (approvalRequired) {
            refund.setStatus(RefundStatus.PENDING_APPROVAL);
            refund.setNextAttemptAt(null);
        } else if (payment.getProviderCode() == ProviderCode.CASH) {
            refund.setStatus(RefundStatus.REQUIRES_ACTION);
            refund.setFailureCode("CASH_REFUND_REQUIRES_MANUAL_SETTLEMENT");
            refund.setFailureMessageSanitized(
                    "Hoàn tiền mặt cần được nhân viên xử lý tại quầy");
        } else {
            refund.setStatus(RefundStatus.REQUESTED);
            refund.setNextAttemptAt(now);
        }
        return refund;
    }

    private PaymentRefund refundForDecision(String refundPublicId, String cinemaPublicId) {
        PaymentRefund existing = refundRepository.findByPublicId(refundPublicId)
                .orElseThrow(() -> refundNotFound(refundPublicId));
        PaymentRefund refund = refundRepository.findByIdForUpdate(existing.getId())
                .orElseThrow(() -> refundNotFound(refundPublicId));
        if (cinemaPublicId != null && !cinemaPublicId.isBlank()) {
            requirePaymentCinema(refund.getPayment(), cinemaPublicId);
        }
        if (refund.getStatus() != RefundStatus.PENDING_APPROVAL) {
            throw new BusinessException(
                    "REFUND_ALREADY_REVIEWED",
                    "Yêu cầu hoàn tiền này đã được xử lý.",
                    HttpStatus.CONFLICT);
        }
        return refund;
    }

    private void requireIndependentReviewer(PaymentRefund refund, Long reviewerAccountId) {
        if (reviewerAccountId != null
                && reviewerAccountId.equals(refund.getRequestedByAccountId())) {
            throw new BusinessException(
                    "REFUND_MAKER_CHECKER",
                    "Người đề nghị hoàn tiền không được tự duyệt yêu cầu của mình.",
                    HttpStatus.CONFLICT);
        }
    }

    private void requirePaymentCinema(Payment payment, String cinemaPublicId) {
        PaymentAnalyticsSnapshot snapshot = snapshotRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new BusinessException(
                        "PAYMENT_SNAPSHOT_MISSING",
                        "Không xác định được rạp của giao dịch.",
                        HttpStatus.CONFLICT));
        String expected = cinemaPublicId == null ? "" : cinemaPublicId.trim().toLowerCase(Locale.ROOT);
        String actual = snapshot.getCinemaPublicId() == null
                ? "" : snapshot.getCinemaPublicId().trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(actual)) {
            throw new BusinessException(
                    "REFUND_CINEMA_SCOPE_DENIED",
                    "Giao dịch không thuộc rạp được phép xử lý.",
                    HttpStatus.FORBIDDEN);
        }
    }

    private void validateRefundablePayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException(
                    "PAYMENT_NOT_REFUNDABLE",
                    "Chỉ giao dịch đã thanh toán thành công mới có thể hoàn tiền",
                    HttpStatus.CONFLICT);
        }
    }

    private void validateAdminRequest(CreateRefundRequest request) {
        if (request.getRefundType() == RefundType.FULL) {
            if (request.getRefundComponent() != RefundComponent.FULL_ORDER) {
                throw new BusinessException(
                        "REFUND_COMPONENT_INVALID",
                        "Hoàn toàn phần phải áp dụng cho toàn bộ đơn",
                        HttpStatus.BAD_REQUEST);
            }
            return;
        }
        if (request.getRefundComponent() == RefundComponent.FULL_ORDER) {
            throw new BusinessException(
                    "TICKET_REFUND_NOT_SUPPORTED",
                    "Chưa hỗ trợ hoàn riêng từng vé hoặc một phần tiền vé",
                    HttpStatus.CONFLICT);
        }
        if (request.getAmount() == null) {
            throw new BusinessException(
                    "REFUND_AMOUNT_REQUIRED",
                    "Vui lòng nhập số tiền cần hoàn",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private BigDecimal refundableRemaining(Payment payment) {
        BigDecimal reserved = refundRepository.sumReservedAmount(
                payment.getId(), RESERVED_STATUSES);
        return payment.getAmount().subtract(reserved);
    }

    private void requireAvailableAmount(BigDecimal amount, BigDecimal remaining) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || remaining == null || amount.compareTo(remaining) > 0) {
            throw new BusinessException(
                    "REFUND_AMOUNT_EXCEEDS_AVAILABLE",
                    "Số tiền hoàn vượt quá số tiền còn có thể hoàn",
                    HttpStatus.CONFLICT);
        }
    }

    private void verifyReplay(PaymentRefund refund, CreateRefundRequest request) {
        BigDecimal expectedAmount = request.getRefundType() == RefundType.FULL
                ? refund.getRequestedAmount() : request.getAmount();
        boolean matchingType = refund.getRefundType() == request.getRefundType()
                || (request.getRefundType() == RefundType.FULL
                    && refund.getRefundType() == RefundType.PARTIAL
                    && refund.getRefundComponent() == RefundComponent.FULL_ORDER);
        if (!matchingType
                || refund.getRefundComponent() != request.getRefundComponent()
                || expectedAmount == null
                || refund.getRequestedAmount().compareTo(expectedAmount) != 0
                || !refund.getReasonCode().equals(normalizeCode(request.getReasonCode()))) {
            throw new BusinessException(
                    "REFUND_IDEMPOTENCY_CONFLICT",
                    "Khóa chống trùng đã được dùng cho một yêu cầu hoàn tiền khác",
                    HttpStatus.CONFLICT);
        }
    }

    private PaymentRefund owned(Long id, String ownerToken) {
        PaymentRefund refund = refundRepository.findByIdForUpdate(id)
                .orElseThrow(() -> refundNotFound(String.valueOf(id)));
        if (!ownerToken.equals(refund.getLockedBy())
                || refund.getLockedUntil() == null
                || refund.getLockedUntil().isBefore(Instant.now())) {
            throw new IllegalStateException("Refund lease owner mismatch");
        }
        return refund;
    }

    private void clearLease(PaymentRefund refund) {
        refund.setLockedBy(null);
        refund.setLockedAt(null);
        refund.setLockedUntil(null);
    }

    private String normalizeKey(String value) {
        String normalized = value.trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }

    private String normalizeCode(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_\\-]", "_");
        return normalized.length() <= 100 ? normalized : normalized.substring(0, 100);
    }

    private String sanitize(String value, int max) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private BusinessException paymentNotFound(String id) {
        return new BusinessException(
                "PAYMENT_NOT_FOUND", "Không tìm thấy giao dịch: " + id, HttpStatus.NOT_FOUND);
    }

    private BusinessException refundNotFound(String id) {
        return new BusinessException(
                "REFUND_NOT_FOUND", "Không tìm thấy yêu cầu hoàn tiền: " + id,
                HttpStatus.NOT_FOUND);
    }

    public record RefundWork(
            Payment payment,
            PaymentRefund refund,
            boolean queryOnly) {
    }
}
