package com.project.paymentservice.dto.response;

import java.time.Instant;

public class CashCancelResponse {
    private Long paymentId;
    private String paymentPublicId;
    private String status;
    private Long cancelledByAccountId;
    private Instant cancelledAt;
    private String bookingDeliveryStatus;

    public CashCancelResponse() {
    }
    public CashCancelResponse(Long paymentId, String status, Long cancelledByAccountId,
            Instant cancelledAt, String bookingDeliveryStatus) {
        this.paymentId = paymentId;
        this.status = status;
        this.cancelledByAccountId = cancelledByAccountId;
        this.cancelledAt = cancelledAt;
        this.bookingDeliveryStatus = bookingDeliveryStatus;
    }
    public Long getPaymentId() { return paymentId; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getStatus() { return status; }
    public Long getCancelledByAccountId() { return cancelledByAccountId; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getBookingDeliveryStatus() { return bookingDeliveryStatus; }
    public void setBookingDeliveryStatus(String value) { this.bookingDeliveryStatus = value; }
}
