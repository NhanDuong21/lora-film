package com.project.promotionservice.benefit.entity;

import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "compensation_vouchers")
public class CompensationVoucher extends BenefitAuditableRecord {

    @Column(name = "voucher_public_id", length = 36, nullable = false)
    private String voucherPublicId;

    @Column(name = "reservation_public_id", length = 36)
    private String reservationPublicId;

    @Column(name = "booking_public_id", length = 36)
    private String bookingPublicId;

    @Column(name = "order_public_id", length = 36)
    private String orderPublicId;

    @Column(name = "user_public_id", length = 36, nullable = false)
    private String userPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_type", length = 50, nullable = false)
    private CompensationType compensationType;

    @Column(name = "reason", length = 255, nullable = false)
    private String reason;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private CompensationStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    public CompensationVoucher() {
    }

    public String getVoucherPublicId() {
        return voucherPublicId;
    }

    public void setVoucherPublicId(String voucherPublicId) {
        this.voucherPublicId = voucherPublicId;
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

    public String getUserPublicId() {
        return userPublicId;
    }

    public void setUserPublicId(String userPublicId) {
        this.userPublicId = userPublicId;
    }

    public CompensationType getCompensationType() {
        return compensationType;
    }

    public void setCompensationType(CompensationType compensationType) {
        this.compensationType = compensationType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public CompensationStatus getStatus() {
        return status;
    }

    public void setStatus(CompensationStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
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
