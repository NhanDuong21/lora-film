package com.project.paymentservice.enumtype;

public enum PaymentLogEventType {
    PAYMENT_CREATED,
    PAYMENT_INITIATED,
    PROVIDER_SESSION_CREATED,
    PROVIDER_CALLBACK_RECEIVED,
    STATUS_CHANGED,
    // Compatibility with payment logs created by the legacy demo/import pipeline.
    PAYMENT_SUCCESS,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    PAYMENT_CANCELLED,
    PAYMENT_EXPIRED,
    LATE_SUCCESS_DETECTED,
    RECONCILIATION_REQUIRED,
    RECONCILIATION_RESOLVED,
    CASH_PAYMENT_COLLECTED
}
