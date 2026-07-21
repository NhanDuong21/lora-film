package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super("RESOURCE_CONFLICT", message, HttpStatus.CONFLICT);
    }

    public ConflictException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
