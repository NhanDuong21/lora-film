package com.lorafilm.booking.common.exception;

import org.springframework.http.HttpStatus;

public class DatabaseException extends BaseException {

    public DatabaseException(String message) {
        super("DATABASE_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public DatabaseException(String message, Throwable cause) {
        super("DATABASE_ERROR", message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
