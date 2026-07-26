package com.project.promotionservice.common.exception;

public final class ErrorCode {
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String ILLEGAL_ARGUMENT = "ILLEGAL_ARGUMENT";
    public static final String INVALID_REQUEST_PARAMETER = "INVALID_REQUEST_PARAMETER";
    public static final String MALFORMED_REQUEST_BODY = "MALFORMED_REQUEST_BODY";
    public static final String CONCURRENCY_CONFLICT = "CONCURRENCY_CONFLICT";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";

    private ErrorCode() {}
}
