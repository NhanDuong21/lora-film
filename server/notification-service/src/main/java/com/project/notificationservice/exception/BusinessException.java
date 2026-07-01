package com.project.notificationservice.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;
    private final Object data;

    public BusinessException(String message, String errorCode, HttpStatus status) {
        this(message, errorCode, status, null);
    }

    public BusinessException(String message, String errorCode, HttpStatus status, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }
}
