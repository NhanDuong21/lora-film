package com.project.notificationservice.provider.model;

public enum ProviderFailureCode {
    PROVIDER_TIMEOUT(true),
    PROVIDER_CONNECTION_FAILED(true),
    PROVIDER_RATE_LIMITED(true),
    PROVIDER_UNAVAILABLE(true),
    INVALID_RECIPIENT(false),
    PROVIDER_AUTH_FAILED(false),
    PROVIDER_REJECTED(false),
    CHANNEL_NOT_SUPPORTED(false);

    private final boolean retryable;

    ProviderFailureCode(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
