package com.project.authservice.exception.common;

/**
 * Thrown when a downstream call times out (e.g., Kafka request-reply timeout).
 * Should map to 504 Gateway Timeout.
 */
public class GatewayTimeoutException extends InfrastructureException {
    public GatewayTimeoutException(String serviceName, Throwable cause) {
        super("Timeout waiting for response from " + serviceName, cause);
    }
    
    public GatewayTimeoutException(String serviceName) {
        super("Timeout waiting for response from " + serviceName);
    }
}
