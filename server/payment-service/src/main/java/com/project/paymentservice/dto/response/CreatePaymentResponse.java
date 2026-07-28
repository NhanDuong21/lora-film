package com.project.paymentservice.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.project.paymentservice.common.MoneyJsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;

public class CreatePaymentResponse {
    private Long paymentId;
    private String paymentPublicId;
    private String bookingPublicId;
    private String paymentTransactionCode;
    private String paymentMethod;
    private String provider;
    private String paymentUrl;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;

    public CreatePaymentResponse() {
    }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String value) { this.paymentTransactionCode = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String value) { this.paymentMethod = value; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
