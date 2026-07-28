package com.project.promotionservice.benefit.entity;

import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "voucher_redemptions")
public class VoucherRedemption extends BenefitAuditableRecord {

    @Column(name = "voucher_public_id", length = 36, nullable = false)
    private String voucherPublicId;

    @Column(name = "campaign_public_id", length = 36)
    private String campaignPublicId;

    @Column(name = "reservation_public_id", length = 36, unique = true)
    private String reservationPublicId;

    @Column(name = "booking_public_id", length = 36)
    private String bookingPublicId;

    @Column(name = "order_public_id", length = 36)
    private String orderPublicId;

    @Column(name = "payment_public_id", length = 36)
    private String paymentPublicId;

    @Column(name = "owner_public_id", length = 36, nullable = false)
    private String ownerPublicId;

    @Column(name = "redeemed_by", length = 36, nullable = false)
    private String redeemedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private RedemptionStatus status;

    @Column(name = "original_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal originalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "rollback_at")
    private Instant rollbackAt;

    @Column(name = "rollback_reason", length = 255)
    private String rollbackReason;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public VoucherRedemption() {
    }

    public String getVoucherPublicId() {
        return voucherPublicId;
    }

    public void setVoucherPublicId(String voucherPublicId) {
        this.voucherPublicId = voucherPublicId;
    }

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
    }

    public String getReservationPublicId() {
        return reservationPublicId;
    }

    public void setReservationPublicId(String reservationPublicId) {
        this.reservationPublicId = reservationPublicId;
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

    public String getOwnerPublicId() {
        return ownerPublicId;
    }

    public void setOwnerPublicId(String ownerPublicId) {
        this.ownerPublicId = ownerPublicId;
    }

    public String getRedeemedBy() {
        return redeemedBy;
    }

    public void setRedeemedBy(String redeemedBy) {
        this.redeemedBy = redeemedBy;
    }

    public RedemptionStatus getStatus() {
        return status;
    }

    public void setStatus(RedemptionStatus status) {
        this.status = status;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
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

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
