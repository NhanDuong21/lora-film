package com.lorafilm.booking.payment.enums;

public enum PaymentEventType {
    PAYMENT_CREATED,
    PAYMENT_PENDING,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_TIMEOUT,
    PAYMENT_CANCELLED,
    REFUND_CREATED,
    REFUND_SUCCESS,
    REFUND_FAILED
}
