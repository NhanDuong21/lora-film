package com.lorafilm.booking.domain.enums;

public enum RetryTaskType {
    OUTBOX_PUBLISH,
    PAYMENT_EVENT_PROCESS,
    INBOX_EVENT_PROCESS,
    BOOKING_EXPIRE,
    SEAT_RELEASE,
    RECONCILIATION
}
