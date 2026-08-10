package com.project.paymentservice.client.booking;

public class BookingPaymentResultResponse {
    private Long bookingId;
    private String bookingPublicId;
    private Long paymentId;
    private String paymentPublicId;
    private String eventId;
    private String bookingStatus;
    private String paymentStatus;
    private Boolean accepted;
    private Boolean idempotent;
    private Boolean reconciliationRequired;
    private String reconciliationTaskPublicId;

    public BookingPaymentResultResponse() {
    }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long value) { this.bookingId = value; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { this.paymentId = value; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getEventId() { return eventId; }
    public void setEventId(String value) { this.eventId = value; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String value) { this.bookingStatus = value; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String value) { this.paymentStatus = value; }
    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean value) { this.accepted = value; }
    public Boolean getIdempotent() { return idempotent; }
    public void setIdempotent(Boolean value) { this.idempotent = value; }
    public Boolean getReconciliationRequired() { return reconciliationRequired; }
    public void setReconciliationRequired(Boolean value) { this.reconciliationRequired = value; }
    public String getReconciliationTaskPublicId() { return reconciliationTaskPublicId; }
    public void setReconciliationTaskPublicId(String value) { this.reconciliationTaskPublicId = value; }
}
