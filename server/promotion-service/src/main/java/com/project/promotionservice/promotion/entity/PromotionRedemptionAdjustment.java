package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_redemption_adjustments")
public class PromotionRedemptionAdjustment extends BaseAuditableEntity {

    @Column(name = "redemption_public_id", length = 36, nullable = false)
    private String redemptionPublicId;

    @Column(name = "reservation_public_id", length = 36, nullable = false)
    private String reservationPublicId;

    @Column(name = "adjustment_type", length = 30, nullable = false)
    private String adjustmentType;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "reason_code", length = 50, nullable = false)
    private String reasonCode;

    @Column(name = "reason", length = 255, nullable = false)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public String getRedemptionPublicId() { return redemptionPublicId; }
    public void setRedemptionPublicId(String value) { this.redemptionPublicId = value; }
    public String getReservationPublicId() { return reservationPublicId; }
    public void setReservationPublicId(String value) { this.reservationPublicId = value; }
    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String value) { this.adjustmentType = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { this.discountAmount = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { this.reasonCode = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }
}
