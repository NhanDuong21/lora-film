package com.project.userservice.exception;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN_ACCESS");
    }
}
