package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a {@code birthday} field does not conform to the required
 * {@code YYYY-MM-DD} (ISO-8601 local date) format.
 *
 * <p>Maps to HTTP {@code 400 Bad Request}.
 */
public class InvalidBirthdayFormatException extends BaseAuthException {

    public InvalidBirthdayFormatException() {
        super("Birthday must be in YYYY-MM-DD format",
              "AUTH_INVALID_BIRTHDAY_FORMAT",
              HttpStatus.BAD_REQUEST);
    }
}
