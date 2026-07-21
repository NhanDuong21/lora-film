package com.lorafilm.booking.infrastructure.enums;

public enum RetryTaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    DEAD_LETTER
}
