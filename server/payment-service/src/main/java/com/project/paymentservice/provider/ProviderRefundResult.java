package com.project.paymentservice.provider;

import java.time.Instant;

public class ProviderRefundResult {
    public enum State {
        SUCCESS,
        PROCESSING,
        FAILED
    }

    private State state;
    private String providerOrderId;
    private String providerRequestId;
    private String providerRefundId;
    private String responseCode;
    private String failureCode;
    private String messageSanitized;
    private String summarySanitized = "{}";
    private Instant occurredAt = Instant.now();
    private Integer retryAfterSeconds;

    public State getState() { return state; }
    public void setState(State value) { this.state = value; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String value) { this.providerOrderId = value; }
    public String getProviderRequestId() { return providerRequestId; }
    public void setProviderRequestId(String value) { this.providerRequestId = value; }
    public String getProviderRefundId() { return providerRefundId; }
    public void setProviderRefundId(String value) { this.providerRefundId = value; }
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String value) { this.responseCode = value; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String value) { this.failureCode = value; }
    public String getMessageSanitized() { return messageSanitized; }
    public void setMessageSanitized(String value) { this.messageSanitized = value; }
    public String getSummarySanitized() { return summarySanitized; }
    public void setSummarySanitized(String value) { this.summarySanitized = value; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Integer value) { this.retryAfterSeconds = value; }
}
