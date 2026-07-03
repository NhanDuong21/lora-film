package com.project.authservice.exception.common;

/**
 * Base class for all infrastructure exceptions (5xx).
 */
public class InfrastructureException extends RuntimeException {
    public InfrastructureException(String message) {
        super(message);
    }
    
    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
