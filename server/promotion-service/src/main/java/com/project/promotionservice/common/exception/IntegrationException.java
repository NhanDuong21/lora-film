package com.project.promotionservice.common.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends BaseException {

    public IntegrationException(String message) {
        super("INTEGRATION_ERROR", message, HttpStatus.BAD_GATEWAY);
    }

    public IntegrationException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_GATEWAY);
    }
}
