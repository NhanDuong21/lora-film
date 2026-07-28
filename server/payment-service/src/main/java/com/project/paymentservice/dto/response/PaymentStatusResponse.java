package com.project.paymentservice.dto.response;

public class PaymentStatusResponse {
    private Long paymentId;
    private String paymentPublicId;
    private String bookingPublicId;
    private String status;
    private String reconciliationStatus;
    private String bookingDeliveryStatus;

    public PaymentStatusResponse() {
    }
    public PaymentStatusResponse(Long paymentId, String status, String reconciliationStatus) {
        this.paymentId = paymentId;
        this.status = status;
        this.reconciliationStatus = reconciliationStatus;
    }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String value) { this.reconciliationStatus = value; }
    public String getBookingDeliveryStatus() { return bookingDeliveryStatus; }
    public void setBookingDeliveryStatus(String value) { this.bookingDeliveryStatus = value; }
}
