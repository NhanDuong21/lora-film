package com.project.paymentservice.provider;

import java.time.Instant;

public class PaymentSession {
    private String providerOrderId;
    private String providerSessionId;
    private String paymentUrl;
    private Instant expiresAt;
    private String sanitizedProviderSummary;

    public PaymentSession() {
    }
    public PaymentSession(String providerOrderId, String providerSessionId,
            String paymentUrl, Instant expiresAt) {
        this.providerOrderId = providerOrderId;
        this.providerSessionId = providerSessionId;
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
    }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String value) { this.providerOrderId = value; }
    public String getProviderSessionId() { return providerSessionId; }
    public void setProviderSessionId(String value) { this.providerSessionId = value; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String value) { this.paymentUrl = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public String getSanitizedProviderSummary() { return sanitizedProviderSummary; }
    public void setSanitizedProviderSummary(String value) { this.sanitizedProviderSummary = value; }
}
