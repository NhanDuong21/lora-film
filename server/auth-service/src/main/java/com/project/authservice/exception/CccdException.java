package com.project.authservice.exception;

import com.project.authservice.exception.common.BusinessValidationException;

public class CccdException {

    public static class InvalidCccdException extends BusinessValidationException {
        public InvalidCccdException() {
            super("CCCD format is invalid", "USER_CCCD_INVALID");
        }
    }

    public static class BirthdayCccdMismatchException extends BusinessValidationException {
        public BirthdayCccdMismatchException() {
            super("Birthday does not match CCCD birth year", "USER_BIRTHDAY_CCCD_MISMATCH");
        }
    }

    public static class CccdAlreadyExistsException extends BusinessValidationException {
        public CccdAlreadyExistsException() {
            super("CCCD already exists", "USER_CCCD_ALREADY_EXISTS");
        }
    }
}
