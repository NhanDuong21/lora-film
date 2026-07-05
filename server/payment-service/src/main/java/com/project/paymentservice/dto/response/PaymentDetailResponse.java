package com.project.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentDetailResponse {

    private Long paymentId;
    private String paymentTransactionCode;
    private Long bookingId;
    private String status;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private Integer attemptNumber;
    private String reconciliationStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public PaymentDetailResponse() {
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String paymentTransactionCode) { this.paymentTransactionCode = paymentTransactionCode; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Integer getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(Integer attemptNumber) { this.attemptNumber = attemptNumber; }

    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
