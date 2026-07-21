package com.lorafilm.booking.infrastructure.enums;

public enum RetryTaskType {
    OUTBOX_PUBLISH,
    PAYMENT_CALLBACK,
    REFUND,
    RECONCILIATION,
    INBOX_PROCESS
}
