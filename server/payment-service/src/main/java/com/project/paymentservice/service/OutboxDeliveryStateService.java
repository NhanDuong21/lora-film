package com.project.paymentservice.service;

import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.entity.PaymentReconciliationCase;
import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.enumtype.OutboxStatus;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import com.project.paymentservice.repository.PaymentReconciliationCaseRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentRefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxDeliveryStateService {
    private final PaymentOutboxEventRepository outboxRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAnalyticsSnapshotRepository snapshotRepository;
    private final PaymentReconciliationCaseRepository reconciliationRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentOutboxService outboxService;
    private final PaymentRuntimeProperties properties;
    private final RefundService refundService;

    public OutboxDeliveryStateService(
            PaymentOutboxEventRepository outboxRepository,
            PaymentRepository paymentRepository,
            PaymentAnalyticsSnapshotRepository snapshotRepository,
            PaymentReconciliationCaseRepository reconciliationRepository,
            PaymentRefundRepository refundRepository,
            PaymentOutboxService outboxService,
            PaymentRuntimeProperties properties,
            RefundService refundService) {
        this.outboxRepository = outboxRepository;
        this.paymentRepository = paymentRepository;
        this.snapshotRepository = snapshotRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.refundRepository = refundRepository;
        this.outboxService = outboxService;
        this.properties = properties;
        this.refundService = refundService;
    }

    @Transactional
    public List<PaymentOutboxEvent> claim(String ownerToken) {
        Instant now = Instant.now();
        return outboxRepository.findAndClaimPendingEvents(
                now,
                now.plusSeconds(properties.getOutboxLeaseSeconds()),
                ownerToken,
                properties.getOutboxBatchSize());
    }

    @Transactional
    public void markBookingPublished(
            Long eventId, String ownerToken, boolean accepted, String reconciliationReason) {
        PaymentOutboxEvent event = owned(eventId, ownerToken);
        Payment payment = paymentRepository.findByPublicId(event.getAggregateId())
                .orElseThrow(() -> new IllegalStateException("Payment aggregate missing"));
        if (!accepted) {
            requireReconciliation(payment, event, reconciliationReason);
            if ("PAYMENT_RESULT".equals(event.getEventType())
                    && payment.getStatus()
                    == com.project.paymentservice.enumtype.PaymentStatus.SUCCESS) {
                refundService.createAutomaticFullRefund(
                        payment.getId(),
                        "automatic:booking-rejected:" + event.getEventId(),
                        "BOOKING_CONFIRMATION_FAILED",
                        "Booking Service không chấp nhận kết quả thanh toán thành công");
            }
        } else if ("PAYMENT_RESULT".equals(event.getEventType())
                && payment.getStatus() == com.project.paymentservice.enumtype.PaymentStatus.SUCCESS
                && payment.getReconciliationStatus() == ReconciliationStatus.NONE) {
            PaymentAnalyticsSnapshot snapshot = snapshotRepository.findByPaymentId(payment.getId())
                    .orElseThrow(() -> new IllegalStateException("Payment analytics snapshot missing"));
            outboxService.enqueueAnalyticsSuccess(payment, snapshot, event.getEventId());
        } else if (accepted && "REFUND_RESULT".equals(event.getEventType())) {
            PaymentRefund refund = refundRepository.findByPublicId(event.getCorrelationId())
                    .orElseThrow(() -> new IllegalStateException("Refund aggregate missing"));
            if (refund.getStatus() == com.project.paymentservice.enumtype.RefundStatus.SUCCESS) {
                PaymentAnalyticsSnapshot snapshot = snapshotRepository.findByPaymentId(payment.getId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Payment analytics snapshot missing"));
                outboxService.enqueueAnalyticsRefund(
                        payment, refund, snapshot, event.getEventId());
            }
        }
        publish(event);
    }

    @Transactional
    public void markPublished(Long eventId, String ownerToken) {
        publish(owned(eventId, ownerToken));
    }

    @Transactional
    public void markFailed(Long eventId, String ownerToken, String safeError) {
        PaymentOutboxEvent event = owned(eventId, ownerToken);
        int attempts = event.getAttemptCount() + 1;
        event.setAttemptCount(attempts);
        event.setLastErrorSanitized(sanitize(safeError));
        event.setLockedBy(null);
        event.setLockedAt(null);
        event.setLockedUntil(null);
        if (attempts >= properties.getOutboxMaxAttempts()) {
            event.setStatus(OutboxStatus.DEAD_LETTER);
            event.setNextRetryAt(null);
        } else {
            event.setStatus(OutboxStatus.FAILED);
            long delay = Math.min(300, 1L << Math.min(attempts, 8));
            event.setNextRetryAt(Instant.now().plusSeconds(delay));
        }
        outboxRepository.save(event);
    }

    @Transactional
    public void replay(String eventId) {
        PaymentOutboxEvent event = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Outbox event not found"));
        event.setStatus(OutboxStatus.PENDING);
        event.setAttemptCount(0);
        event.setNextRetryAt(Instant.now());
        event.setLastErrorSanitized(null);
        event.setLockedBy(null);
        event.setLockedAt(null);
        event.setLockedUntil(null);
        outboxRepository.save(event);
    }

    private PaymentOutboxEvent owned(Long id, String ownerToken) {
        PaymentOutboxEvent event = outboxRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Outbox event missing"));
        if (event.getStatus() != OutboxStatus.PROCESSING
                || !ownerToken.equals(event.getLockedBy())) {
            throw new IllegalStateException("Outbox lease owner mismatch");
        }
        return event;
    }

    private void publish(PaymentOutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLockedBy(null);
        event.setLockedAt(null);
        event.setLockedUntil(null);
        event.setLastErrorSanitized(null);
        outboxRepository.save(event);
    }

    private void requireReconciliation(
            Payment payment, PaymentOutboxEvent event, String reason) {
        String code = reason == null ? "BOOKING_RESULT_REJECTED" : reason;
        payment.setReconciliationStatus(ReconciliationStatus.REQUIRED);
        payment.setReconciliationReason(code);
        paymentRepository.save(payment);
        if (reconciliationRepository.findByPaymentIdAndReasonCodeAndSourceReference(
                payment.getId(), code, event.getEventId()).isPresent()) {
            return;
        }
        PaymentReconciliationCase item = new PaymentReconciliationCase();
        item.setPublicId(UUID.randomUUID().toString());
        item.setPaymentId(payment.getId());
        item.setReasonCode(code);
        item.setSourceReference(event.getEventId());
        item.setDetailSanitized("Booking Service did not accept the Payment result");
        reconciliationRepository.save(item);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "DELIVERY_FAILED";
        }
        String result = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return result.length() <= 2000 ? result : result.substring(0, 2000);
    }
}
