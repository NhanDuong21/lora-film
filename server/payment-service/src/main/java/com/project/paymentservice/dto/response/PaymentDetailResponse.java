package com.project.paymentservice.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.project.paymentservice.common.MoneyJsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDetailResponse {
    private Long paymentId;
    private String paymentPublicId;
    private String paymentTransactionCode;
    private Long bookingId;
    private String bookingPublicId;
    private String status;
    private String paymentMethod;
    private String provider;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal amount;
    private String currency;
    private Integer attemptNumber;
    private String reconciliationStatus;
    private String bookingDeliveryStatus;
    private String externalTransactionId;
    private String movieTitle;
    private Integer ticketCount;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal ticketAmount;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal foodAmount;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal discountAmount;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public PaymentDetailResponse() {
    }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String value) { this.paymentTransactionCode = value; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { this.paymentMethod = value; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String value) { this.reconciliationStatus = value; }
    public String getBookingDeliveryStatus() { return bookingDeliveryStatus; }
    public void setBookingDeliveryStatus(String value) { this.bookingDeliveryStatus = value; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String value) { this.externalTransactionId = value; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String value) { this.movieTitle = value; }
    public Integer getTicketCount() { return ticketCount; }
    public void setTicketCount(Integer value) { this.ticketCount = value; }
    public BigDecimal getTicketAmount() { return ticketAmount; }
    public void setTicketAmount(BigDecimal value) { this.ticketAmount = value; }
    public BigDecimal getFoodAmount() { return foodAmount; }
    public void setFoodAmount(BigDecimal value) { this.foodAmount = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { this.discountAmount = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
