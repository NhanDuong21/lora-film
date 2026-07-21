package com.lorafilm.booking.domain.enums;

public enum RetryReferenceType {
    BOOKING,
    OUTBOX_EVENT,
    INBOX_EVENT,
    PAYMENT_EVENT,
    SEAT_RESERVATION,
    RECONCILIATION_TASK
}
