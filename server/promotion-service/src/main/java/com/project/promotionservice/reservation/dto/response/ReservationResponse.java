package com.project.promotionservice.reservation.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.promotion.dto.response.AppliedPromotionResponse;
import com.project.promotionservice.reservation.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ReservationResponse {

    private String publicId;
    private String reservationCode;
    private ReservationStatus status;
    private String bookingPublicId;
    private String orderPublicId;
    private String paymentPublicId;
    private String userPublicId;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String currency;
    private Instant reservationStartedAt;
    private Instant reservationExpiredAt;
    private Instant confirmedAt;
    private Instant rollbackAt;
    private String rollbackReason;
    private JsonNode contextJson;
    private List<AppliedPromotionResponse> appliedPromotions = List.of();

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
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

    public JsonNode getContextJson() {
        return contextJson;
    }

    public void setContextJson(JsonNode contextJson) {
        this.contextJson = contextJson;
    }

    public List<AppliedPromotionResponse> getAppliedPromotions() {
        return appliedPromotions;
    }

    public void setAppliedPromotions(List<AppliedPromotionResponse> appliedPromotions) {
        this.appliedPromotions = appliedPromotions;
    }
}
