package com.project.paymentservice.entity;

import com.project.paymentservice.enumtype.ReconciliationCaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_reconciliation_cases")
public class PaymentReconciliationCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, columnDefinition = "char(36)")
    private String publicId;
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "webhook_event_id")
    private Long webhookEventId;
    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;
    @Column(name = "source_reference", length = 150)
    private String sourceReference;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReconciliationCaseStatus status = ReconciliationCaseStatus.OPEN;
    @Column(name = "detail_sanitized", columnDefinition = "text")
    private String detailSanitized;
    @Column(name = "assigned_to_account_id")
    private Long assignedToAccountId;
    @Column(name = "resolution_code", length = 100)
    private String resolutionCode;
    @Column(name = "resolution_note_sanitized", columnDefinition = "text")
    private String resolutionNoteSanitized;
    @Column(name = "resolved_by_account_id")
    private Long resolvedByAccountId;
    @Column(name = "opened_at", insertable = false, updatable = false)
    private Instant openedAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public PaymentReconciliationCase() {
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getWebhookEventId() { return webhookEventId; }
    public void setWebhookEventId(Long webhookEventId) { this.webhookEventId = webhookEventId; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public ReconciliationCaseStatus getStatus() { return status; }
    public void setStatus(ReconciliationCaseStatus status) { this.status = status; }
    public String getDetailSanitized() { return detailSanitized; }
    public void setDetailSanitized(String detailSanitized) { this.detailSanitized = detailSanitized; }
    public Long getAssignedToAccountId() { return assignedToAccountId; }
    public void setAssignedToAccountId(Long value) { this.assignedToAccountId = value; }
    public String getResolutionCode() { return resolutionCode; }
    public void setResolutionCode(String resolutionCode) { this.resolutionCode = resolutionCode; }
    public String getResolutionNoteSanitized() { return resolutionNoteSanitized; }
    public void setResolutionNoteSanitized(String value) { this.resolutionNoteSanitized = value; }
    public Long getResolvedByAccountId() { return resolvedByAccountId; }
    public void setResolvedByAccountId(Long value) { this.resolvedByAccountId = value; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
