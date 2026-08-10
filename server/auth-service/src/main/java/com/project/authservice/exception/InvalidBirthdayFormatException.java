package com.project.authservice.exception;

import com.project.authservice.exception.common.BusinessValidationException;

/**
 * Thrown when a {@code birthday} field does not conform to the required
 * {@code YYYY-MM-DD} (ISO-8601 local date) format.
 *
 * <p>Maps to HTTP {@code 422 Unprocessable Content}.
 */
public class InvalidBirthdayFormatException extends BusinessValidationException {

    public InvalidBirthdayFormatException() {
        super("Birthday must be in YYYY-MM-DD format", "AUTH_INVALID_BIRTHDAY_FORMAT");
    }

    public InvalidBirthdayFormatException(String message) {
        super(message, null);
    }
}
