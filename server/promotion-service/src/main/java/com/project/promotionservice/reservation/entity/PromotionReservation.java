package com.project.promotionservice.reservation.entity;

import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.common.entity.BaseAuditableEntity;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotion_reservations")
public class PromotionReservation extends BaseAuditableEntity {

    @Column(name = "reservation_code", length = 100, nullable = false, unique = true)
    private String reservationCode;

    @Column(name = "booking_public_id", length = 36)
    private String bookingPublicId;

    @Column(name = "order_public_id", length = 36)
    private String orderPublicId;

    @Column(name = "payment_public_id", length = 36)
    private String paymentPublicId;

    @Column(name = "user_public_id", length = 36, nullable = false)
    private String userPublicId;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "reservation_scope_key", length = 80, unique = true)
    private String reservationScopeKey;

    @Column(name = "campaign_public_id", length = 36)
    private String campaignPublicId;

    @Column(name = "coupon_public_id", length = 36)
    private String couponPublicId;

    @Column(name = "voucher_public_id", length = 36)
    private String voucherPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", length = 30, nullable = false)
    private RedemptionType reservationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ReservationStatus status;

    @Column(name = "original_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal originalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal finalAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 10, nullable = false)
    private String currency = "VND";

    @Column(name = "reservation_started_at", nullable = false)
    private Instant reservationStartedAt;

    @Column(name = "reservation_expired_at", nullable = false)
    private Instant reservationExpiredAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_reason", length = 255)
    private String cancelledReason;

    @Column(name = "rollback_at")
    private Instant rollbackAt;

    @Column(name = "rollback_reason", length = 255)
    private String rollbackReason;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @Column(name = "expiration_attempts", nullable = false)
    private Integer expirationAttempts = 0;

    @Column(name = "expiration_last_attempt_at")
    private Instant expirationLastAttemptAt;

    @Column(name = "expiration_next_attempt_at")
    private Instant expirationNextAttemptAt;

    @Column(name = "expiration_error", length = 1000)
    private String expirationError;

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
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

    public String getReservationScopeKey() {
        return reservationScopeKey;
    }

    public void setReservationScopeKey(String reservationScopeKey) {
        this.reservationScopeKey = reservationScopeKey;
    }

    public String getCampaignPublicId() {
        return campaignPublicId;
    }

    public void setCampaignPublicId(String campaignPublicId) {
        this.campaignPublicId = campaignPublicId;
    }

    public String getCouponPublicId() {
        return couponPublicId;
    }

    public void setCouponPublicId(String couponPublicId) {
        this.couponPublicId = couponPublicId;
    }

    public String getVoucherPublicId() {
        return voucherPublicId;
    }

    public void setVoucherPublicId(String voucherPublicId) {
        this.voucherPublicId = voucherPublicId;
    }

    public RedemptionType getReservationType() {
        return reservationType;
    }

    public void setReservationType(RedemptionType reservationType) {
        this.reservationType = reservationType;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getReservationStartedAt() {
        return reservationStartedAt;
    }

    public void setReservationStartedAt(Instant reservationStartedAt) {
        this.reservationStartedAt = reservationStartedAt;
    }

    public Instant getReservationExpiredAt() {
        return reservationExpiredAt;
    }

    public void setReservationExpiredAt(Instant reservationExpiredAt) {
        this.reservationExpiredAt = reservationExpiredAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledReason() {
        return cancelledReason;
    }

    public void setCancelledReason(String cancelledReason) {
        this.cancelledReason = cancelledReason;
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

    public Integer getExpirationAttempts() {
        return expirationAttempts;
    }

    public void setExpirationAttempts(Integer expirationAttempts) {
        this.expirationAttempts = expirationAttempts;
    }

    public Instant getExpirationLastAttemptAt() {
        return expirationLastAttemptAt;
    }

    public void setExpirationLastAttemptAt(Instant expirationLastAttemptAt) {
        this.expirationLastAttemptAt = expirationLastAttemptAt;
    }

    public Instant getExpirationNextAttemptAt() {
        return expirationNextAttemptAt;
    }

    public void setExpirationNextAttemptAt(Instant expirationNextAttemptAt) {
        this.expirationNextAttemptAt = expirationNextAttemptAt;
    }

    public String getExpirationError() {
        return expirationError;
    }

    public void setExpirationError(String expirationError) {
        this.expirationError = expirationError;
    }
}
