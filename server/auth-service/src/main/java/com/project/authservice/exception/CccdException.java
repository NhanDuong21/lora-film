package com.project.authservice.exception;

import org.springframework.http.HttpStatus;

public class CccdException {

    public static class InvalidCccdException extends BaseAuthException {
        public InvalidCccdException() {
            super("CCCD format is invalid", "USER_CCCD_INVALID", HttpStatus.BAD_REQUEST);
        }
    }

    public static class BirthdayCccdMismatchException extends BaseAuthException {
        public BirthdayCccdMismatchException() {
            super("Birthday does not match CCCD birth year", "USER_BIRTHDAY_CCCD_MISMATCH", HttpStatus.BAD_REQUEST);
        }
    }

    public static class CccdAlreadyExistsException extends BaseAuthException {
        public CccdAlreadyExistsException() {
            super("CCCD already exists", "USER_CCCD_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }
}
