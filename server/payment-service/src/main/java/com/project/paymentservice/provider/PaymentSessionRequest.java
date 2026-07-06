package com.project.paymentservice.provider;

import java.math.BigDecimal;

public class PaymentSessionRequest {

    private Long paymentId;
    private String paymentTransactionCode;
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private String returnUrl;

    public PaymentSessionRequest() {
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String paymentTransactionCode) { this.paymentTransactionCode = paymentTransactionCode; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }
}
