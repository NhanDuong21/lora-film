package com.lorafilm.booking.domain.enums;

public enum IdempotencyStatus {
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED
}
