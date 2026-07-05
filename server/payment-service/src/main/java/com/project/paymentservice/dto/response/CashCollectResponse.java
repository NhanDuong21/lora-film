package com.project.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CashCollectResponse {
    private Long paymentId;
    private Long bookingId;
    private String paymentMethod;
    private String status;
    private BigDecimal amount;
    private BigDecimal receivedAmount;
    private BigDecimal changeAmount;
    private Long collectedByAccountId;
    private LocalDateTime collectedAt;

    public CashCollectResponse(Long paymentId, Long bookingId, String paymentMethod, String status, BigDecimal amount, BigDecimal receivedAmount, BigDecimal changeAmount, Long collectedByAccountId, LocalDateTime collectedAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.amount = amount;
        this.receivedAmount = receivedAmount;
        this.changeAmount = changeAmount;
        this.collectedByAccountId = collectedByAccountId;
        this.collectedAt = collectedAt;
    }

    public Long getPaymentId() { return paymentId; }
    public Long getBookingId() { return bookingId; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public Long getCollectedByAccountId() { return collectedByAccountId; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
