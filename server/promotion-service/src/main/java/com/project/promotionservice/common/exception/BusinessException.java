package com.project.promotionservice.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super("BUSINESS_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, status);
    }
}
