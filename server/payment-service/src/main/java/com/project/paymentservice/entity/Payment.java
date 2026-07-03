package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_transaction_code", unique = true, nullable = false, length = 100)
    private String paymentTransactionCode;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "provider_order_id", length = 150)
    private String providerOrderId;

    @Column(name = "provider_session_id", length = 150)
    private String providerSessionId;

    @Column(name = "external_transaction_id", length = 150)
    private String externalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", nullable = false, length = 30)
    @Builder.Default
    private ReconciliationStatus reconciliationStatus = ReconciliationStatus.NONE;

    @Column(name = "reconciliation_reason", length = 255)
    private String reconciliationReason;

    @Column(name = "reconciliation_resolved_at")
    private LocalDateTime reconciliationResolvedAt;

    @Column(name = "settlement_hold_until")
    private LocalDateTime settlementHoldUntil;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message_sanitized", columnDefinition = "TEXT")
    private String failureMessageSanitized;

    @Column(name = "provider_response_code", length = 100)
    private String providerResponseCode;

    @Column(name = "succeeded_at")
    private LocalDateTime succeededAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "latest_provider_summary_sanitized", columnDefinition = "TEXT")
    private String latestProviderSummarySanitized;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
