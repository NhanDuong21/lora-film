package com.project.promotionservice.automation.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/** Tombstone that prevents an older confirmation from undoing a later refund. */
@Entity
@Table(name = "promotion_automation_suppressions", uniqueConstraints =
        @UniqueConstraint(name = "uq_automation_suppression_trigger",
                columnNames = {"playbook_code", "trigger_reference"}))
public class PromotionAutomationSuppression extends BaseAuditableEntity {
    @Column(name = "playbook_code", nullable = false, length = 80)
    private String playbookCode;
    @Column(name = "trigger_reference", nullable = false, length = 180)
    private String triggerReference;
    @Column(name = "reason_code", nullable = false, length = 100)
    private String reasonCode;
    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    public String getPlaybookCode() { return playbookCode; }
    public void setPlaybookCode(String value) { playbookCode = value; }
    public String getTriggerReference() { return triggerReference; }
    public void setTriggerReference(String value) { triggerReference = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { reasonCode = value; }
    public Instant getObservedAt() { return observedAt; }
    public void setObservedAt(Instant value) { observedAt = value; }
}
