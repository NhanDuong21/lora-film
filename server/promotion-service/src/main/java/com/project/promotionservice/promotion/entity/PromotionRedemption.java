package com.project.promotionservice.promotion.entity;

import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_redemptions")
public class PromotionRedemption extends BaseAuditableEntity {

    @Column(name = "reservation_public_id", length = 36)
    private String reservationPublicId;

    @Column(name = "user_public_id", length = 36, nullable = false)
    private String userPublicId;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "promotion_public_id", length = 36, nullable = false)
    private String promotionPublicId;

    @Column(name = "campaign_public_id", length = 36, nullable = false)
    private String campaignPublicId;
    @Column(name = "test_data", nullable = false)
    private Boolean testData = false;
    @Column(name = "environment_tag", nullable = false, length = 30)
    private String environmentTag = "BUSINESS";

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", length = 30, nullable = false)
    private PromotionType promotionType;

    @Column(name = "promotion_code", length = 100)
    private String promotionCode;

    @Column(name = "promotion_name", length = 255, nullable = false)
    private String promotionName;

    @Column(name = "promotion_priority", nullable = false)
    private Integer promotionPriority;

    @Column(name = "promotion_stackable", nullable = false)
    private Boolean promotionStackable;

    @Column(name = "conditions_snapshot_json", columnDefinition = "json", nullable = false)
    private String conditionsSnapshotJson;

    @Column(name = "actions_snapshot_json", columnDefinition = "json", nullable = false)
    private String actionsSnapshotJson;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "user_promotion_public_id", length = 36)
    private String userPromotionPublicId;

    @Column(name = "booking_public_id", length = 36)
    private String bookingPublicId;

    @Column(name = "order_public_id", length = 36)
    private String orderPublicId;

    @Column(name = "payment_public_id", length = 36)
    private String paymentPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private PromotionRedemptionStatus status;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "original_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal originalAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "rollback_at")
    private Instant rollbackAt;

    @Column(name = "rollback_reason", length = 255)
    private String rollbackReason;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    public String getReservationPublicId() {
        return reservationPublicId;
    }

    public void setReservationPublicId(String reservationPublicId) {
        this.reservationPublicId = reservationPublicId;
    }

    public String getUserPublicId() {
        return userPublicId;
    }

    public void setUserPublicId(String userPublicId) {
        this.userPublicId = userPublicId;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPromotionPublicId() {
        return promotionPublicId;
    }

    public void setPromotionPublicId(String promotionPublicId) {
        this.promotionPublicId = promotionPublicId;
    }

    public String getCampaignPublicId() { return campaignPublicId; }
    public void setCampaignPublicId(String value) { this.campaignPublicId = value; }
    public Boolean getTestData() { return testData; }
    public void setTestData(Boolean value) { testData = value; }
    public String getEnvironmentTag() { return environmentTag; }
    public void setEnvironmentTag(String value) { environmentTag = value; }
    public PromotionType getPromotionType() { return promotionType; }
    public void setPromotionType(PromotionType value) { this.promotionType = value; }
    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String value) { this.promotionCode = value; }
    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String value) { this.promotionName = value; }
    public Integer getPromotionPriority() { return promotionPriority; }
    public void setPromotionPriority(Integer value) { this.promotionPriority = value; }
    public Boolean getPromotionStackable() { return promotionStackable; }
    public void setPromotionStackable(Boolean value) { this.promotionStackable = value; }
    public String getConditionsSnapshotJson() { return conditionsSnapshotJson; }
    public void setConditionsSnapshotJson(String value) { this.conditionsSnapshotJson = value; }
    public String getActionsSnapshotJson() { return actionsSnapshotJson; }
    public void setActionsSnapshotJson(String value) { this.actionsSnapshotJson = value; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer value) { this.sequenceNo = value; }

    public String getUserPromotionPublicId() {
        return userPromotionPublicId;
    }

    public void setUserPromotionPublicId(String userPromotionPublicId) {
        this.userPromotionPublicId = userPromotionPublicId;
    }

    public String getBookingPublicId() {
        return bookingPublicId;
    }

    public void setBookingPublicId(String bookingPublicId) {
        this.bookingPublicId = bookingPublicId;
    }

    public String getOrderPublicId() {
        return orderPublicId;
    }

    public void setOrderPublicId(String orderPublicId) {
        this.orderPublicId = orderPublicId;
    }

    public String getPaymentPublicId() {
        return paymentPublicId;
    }

    public void setPaymentPublicId(String paymentPublicId) {
        this.paymentPublicId = paymentPublicId;
    }

    public PromotionRedemptionStatus getStatus() {
        return status;
    }

    public void setStatus(PromotionRedemptionStatus status) {
        this.status = status;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getRollbackAt() {
        return rollbackAt;
    }

    public void setRollbackAt(Instant rollbackAt) {
        this.rollbackAt = rollbackAt;
    }

    public String getRollbackReason() {
        return rollbackReason;
    }

    public void setRollbackReason(String rollbackReason) {
        this.rollbackReason = rollbackReason;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
