package com.project.authservice.exception.common;

/**
 * Base class for all business validation exceptions that should return a 422 Unprocessable Content.
 */
public class BusinessValidationException extends RuntimeException {
    private final String errorCode;

    public BusinessValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessValidationException(String message) {
        super(message);
        this.errorCode = "BUSINESS_VALIDATION_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
