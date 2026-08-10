package com.project.authservice.exception.common;

/**
 * Thrown when an external service (e.g., Redis, Notification Service) is unavailable.
 * Should map to 503 Service Unavailable.
 */
public class ExternalServiceUnavailableException extends InfrastructureException {
    public ExternalServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is currently unavailable.", cause);
    }
    
    public ExternalServiceUnavailableException(String serviceName) {
        super(serviceName + " is currently unavailable.");
    }
}
