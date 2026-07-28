package com.project.paymentservice.dto.request;

public class CreateCashPaymentRequest {
    private String bookingPublicId;
    private String bookingCode;

    public CreateCashPaymentRequest() {
    }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String value) { this.bookingCode = value; }
}
