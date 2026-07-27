package com.lorafilm.booking.payment.event.contract;

import java.math.BigDecimal;

public record PaymentEventPayload(
    Long paymentId,
    String paymentPublicId,
    Long bookingId,
    String bookingPublicId,
    String transactionCode,
    String paymentProvider,
    String paymentMethod,
    String paymentStatus,
    BigDecimal amount,
    String currency,
    String externalTransactionId,
    String errorCode,
    String errorMessage
) {}
