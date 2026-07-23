package com.lorafilm.booking.payment.dto;

import java.math.BigDecimal;

public record PaymentResponseDto(
    Long paymentId,
    Long bookingId,
    String transactionCode,
    String paymentStatus,
    BigDecimal amount,
    String currency,
    String paymentUrl,
    String externalTransactionId
) {}
