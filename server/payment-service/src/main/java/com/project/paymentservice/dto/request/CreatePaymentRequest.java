package com.project.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreatePaymentRequest {
    @Schema(description = "Canonical Booking public UUID")
    private String bookingPublicId;
    @Schema(description = "Deprecated numeric Booking identifier")
    @Positive
    private Long bookingId;
    @Schema(allowableValues = {"VNPAY", "MOMO", "MOCK"})
    @NotBlank(message = "paymentMethod is required")
    private String paymentMethod;

    public CreatePaymentRequest() {
    }

    /** Compatibility constructor. */
    public CreatePaymentRequest(Long bookingId, String paymentMethod) {
        this.bookingId = bookingId;
        this.paymentMethod = paymentMethod;
    }

    public CreatePaymentRequest(String bookingPublicId, String paymentMethod) {
        this.bookingPublicId = bookingPublicId;
        this.paymentMethod = paymentMethod;
    }

    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String bookingPublicId) { this.bookingPublicId = bookingPublicId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
