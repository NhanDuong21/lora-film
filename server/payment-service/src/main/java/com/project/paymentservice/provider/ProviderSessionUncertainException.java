package com.project.paymentservice.provider;

public class ProviderSessionUncertainException extends RuntimeException {
    private final String providerOrderId;
    private final String providerSessionId;
    private final String sanitizedSummary;

    public ProviderSessionUncertainException(
            String message,
            String providerOrderId,
            String providerSessionId,
            String sanitizedSummary,
            Throwable cause) {
        super(message, cause);
        this.providerOrderId = providerOrderId;
        this.providerSessionId = providerSessionId;
        this.sanitizedSummary = sanitizedSummary;
    }

    public String getProviderOrderId() {
        return providerOrderId;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public String getSanitizedSummary() {
        return sanitizedSummary;
    }
}
