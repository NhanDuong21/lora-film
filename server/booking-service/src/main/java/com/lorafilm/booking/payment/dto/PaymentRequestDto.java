package com.lorafilm.booking.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequestDto(
    Long bookingId,
    String bookingCode,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String paymentProvider,
    Long userId,
    Instant expiresAt
) {}
