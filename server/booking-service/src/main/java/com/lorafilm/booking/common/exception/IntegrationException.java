package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends BaseException {

    public IntegrationException(String message) {
        super("INTEGRATION_ERROR", message, HttpStatus.BAD_GATEWAY);
    }

    public IntegrationException(String message, Throwable cause) {
        super("INTEGRATION_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
    }
}
