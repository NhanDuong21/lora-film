package com.project.paymentservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "accounting_audit_events")
public class AccountingAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "action_code", nullable = false, length = 80)
    private String actionCode;
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;
    @Column(name = "aggregate_public_id", nullable = false, length = 100)
    private String aggregatePublicId;
    @Column(name = "actor_account_id", nullable = false)
    private Long actorAccountId;
    @Column(name = "detail_sanitized", length = 2000)
    private String detailSanitized;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String value) { actionCode = value; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String value) { aggregateType = value; }
    public String getAggregatePublicId() { return aggregatePublicId; }
    public void setAggregatePublicId(String value) { aggregatePublicId = value; }
    public Long getActorAccountId() { return actorAccountId; }
    public void setActorAccountId(Long value) { actorAccountId = value; }
    public String getDetailSanitized() { return detailSanitized; }
    public void setDetailSanitized(String value) { detailSanitized = value; }
    public Instant getCreatedAt() { return createdAt; }
}
