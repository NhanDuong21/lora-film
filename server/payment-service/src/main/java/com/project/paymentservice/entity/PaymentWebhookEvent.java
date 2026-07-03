package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.WebhookProcessingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_event_id", length = 150)
    private String providerEventId;

    @Column(name = "deduplication_key", nullable = false, length = 150)
    private String deduplicationKey;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_transaction_code", length = 100)
    private String paymentTransactionCode;

    @Column(name = "provider_order_id", length = 150)
    private String providerOrderId;

    @Column(name = "external_transaction_id", length = 150)
    private String externalTransactionId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "payload_hash", nullable = false, length = 255)
    private String payloadHash;

    @Column(name = "sanitized_payload", nullable = false, columnDefinition = "TEXT")
    private String sanitizedPayload;

    @Column(name = "signature_valid", nullable = false)
    @Builder.Default
    private Boolean signatureValid = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    @Builder.Default
    private WebhookProcessingStatus processingStatus = WebhookProcessingStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "received_at", insertable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
