package com.project.paymentservice.provider;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentSessionRequest {
    private Long paymentId;
    private String paymentPublicId;
    private String paymentTransactionCode;
    private Long bookingId;
    private String bookingPublicId;
    private BigDecimal amount;
    private String currency;
    private String clientIp;
    private String orderDescription;
    private Instant expiresAt;

    public PaymentSessionRequest() {
    }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { this.paymentId = value; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String value) { this.paymentTransactionCode = value; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { this.bookingId = value; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String value) { this.clientIp = value; }
    public String getOrderDescription() { return orderDescription; }
    public void setOrderDescription(String value) { this.orderDescription = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
}
