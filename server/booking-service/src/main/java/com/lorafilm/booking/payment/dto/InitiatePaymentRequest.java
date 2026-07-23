package com.lorafilm.booking.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record InitiatePaymentRequest(
    @NotBlank(message = "paymentMethod is required")
    String paymentMethod,
    
    @NotBlank(message = "paymentProvider is required")
    String paymentProvider
) {}
