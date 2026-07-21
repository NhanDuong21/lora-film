package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super("RESOURCE_NOT_FOUND", String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue), HttpStatus.NOT_FOUND);
    }
}
