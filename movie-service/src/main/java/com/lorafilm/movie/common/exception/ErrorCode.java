package com.lorafilm.movie.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    RESOURCE_NOT_FOUND(404, "Resource Not Found"),
    VALIDATION_ERROR(400, "Validation Error"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
