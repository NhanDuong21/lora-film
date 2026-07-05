package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreatePaymentRequest {

    @Schema(description = "ID of the booking to pay for", example = "1001")
    @NotNull(message = "bookingId is required")
    @Positive(message = "bookingId must be positive")
    private Long bookingId;

    @Schema(description = "Payment method provider", example = "MOCK", allowableValues = {"MOCK", "VNPAY", "MOMO"})
    @NotNull(message = "paymentMethod is required")
    private String paymentMethod;

    public CreatePaymentRequest() {
    }

    public CreatePaymentRequest(Long bookingId, String paymentMethod) {
        this.bookingId = bookingId;
        this.paymentMethod = paymentMethod;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
