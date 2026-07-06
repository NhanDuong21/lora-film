package com.project.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private Object data;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = null;
    }

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String errorCode, String message, HttpStatus httpStatus, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Object getData() {
        return data;
    }
}
