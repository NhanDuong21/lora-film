package com.project.notificationservice.provider.model;

public class ProviderSendResult {
    private boolean success;
    private String providerName;
    private String providerMessageId;
    private String failureCode;
    private String errorMessage;
    private boolean retryable;

    public ProviderSendResult() {
    }

    public ProviderSendResult(boolean success, String providerName, String providerMessageId,
                              String failureCode, String errorMessage, boolean retryable) {
        this.success = success;
        this.providerName = providerName;
        this.providerMessageId = providerMessageId;
        this.failureCode = failureCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String providerName;
        private String providerMessageId;
        private String failureCode;
        private String errorMessage;
        private boolean retryable;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder providerMessageId(String providerMessageId) {
            this.providerMessageId = providerMessageId;
            return this;
        }

        public Builder failureCode(String failureCode) {
            this.failureCode = failureCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder retryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        public ProviderSendResult build() {
            return new ProviderSendResult(success, providerName, providerMessageId, failureCode, errorMessage, retryable);
        }
    }
}
