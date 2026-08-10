package com.project.analyticsservice.exception;

public class NonRetryableAnalyticsEventException extends RuntimeException {
    public NonRetryableAnalyticsEventException(String message) {
        super(message);
    }

    public NonRetryableAnalyticsEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
