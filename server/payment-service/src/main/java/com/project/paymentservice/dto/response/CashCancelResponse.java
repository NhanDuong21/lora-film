package com.project.paymentservice.dto.response;

import java.time.LocalDateTime;

public class CashCancelResponse {
    private Long paymentId;
    private String status;
    private Long cancelledByAccountId;
    private LocalDateTime cancelledAt;
    private String bookingDeliveryStatus;

    public CashCancelResponse(Long paymentId, String status, Long cancelledByAccountId, LocalDateTime cancelledAt, String bookingDeliveryStatus) {
        this.paymentId = paymentId;
        this.status = status;
        this.cancelledByAccountId = cancelledByAccountId;
        this.cancelledAt = cancelledAt;
        this.bookingDeliveryStatus = bookingDeliveryStatus;
    }

    public Long getPaymentId() { return paymentId; }
    public String getStatus() { return status; }
    public Long getCancelledByAccountId() { return cancelledByAccountId; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getBookingDeliveryStatus() { return bookingDeliveryStatus; }
    public void setBookingDeliveryStatus(String bookingDeliveryStatus) { this.bookingDeliveryStatus = bookingDeliveryStatus; }
}
