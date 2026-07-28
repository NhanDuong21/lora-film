package com.project.paymentservice.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.project.paymentservice.common.MoneyJsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;

public class CashCollectResponse {
    private Long paymentId;
    private String paymentPublicId;
    private Long bookingId;
    private String bookingPublicId;
    private String paymentMethod;
    private String status;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal amount;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal receivedAmount;
    @JsonSerialize(using = MoneyJsonSerializer.class)
    private BigDecimal changeAmount;
    private Long collectedByAccountId;
    private Instant collectedAt;
    private String bookingDeliveryStatus;

    public CashCollectResponse() {
    }
    public CashCollectResponse(Long paymentId, Long bookingId, String paymentMethod, String status,
            BigDecimal amount, BigDecimal receivedAmount, BigDecimal changeAmount,
            Long collectedByAccountId, Instant collectedAt, String bookingDeliveryStatus) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.amount = amount;
        this.receivedAmount = receivedAmount;
        this.changeAmount = changeAmount;
        this.collectedByAccountId = collectedByAccountId;
        this.collectedAt = collectedAt;
        this.bookingDeliveryStatus = bookingDeliveryStatus;
    }
    public Long getPaymentId() { return paymentId; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public Long getBookingId() { return bookingId; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public Long getCollectedByAccountId() { return collectedByAccountId; }
    public Instant getCollectedAt() { return collectedAt; }
    public String getBookingDeliveryStatus() { return bookingDeliveryStatus; }
    public void setBookingDeliveryStatus(String value) { this.bookingDeliveryStatus = value; }
}
