package com.lorafilm.booking.payment.event.contract;

import java.math.BigDecimal;

public record PaymentEventPayload(
    Long paymentId,
    Long bookingId,
    String transactionCode,
    String paymentMethod,
    String paymentStatus,
    BigDecimal amount,
    String currency,
    String externalTransactionId,
    String errorCode,
    String errorMessage
) {}
