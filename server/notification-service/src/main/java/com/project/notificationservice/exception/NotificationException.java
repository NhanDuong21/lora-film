package com.project.notificationservice.exception;

import org.springframework.http.HttpStatus;

public class NotificationException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public NotificationException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
