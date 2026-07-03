package com.project.paymentservice.provider;

import java.time.LocalDateTime;

public class PaymentSession {

    private String providerOrderId;
    private String providerSessionId;
    private String paymentUrl;
    private LocalDateTime expiresAt;

    public PaymentSession() {
    }

    public PaymentSession(String providerOrderId, String providerSessionId,
                          String paymentUrl, LocalDateTime expiresAt) {
        this.providerOrderId = providerOrderId;
        this.providerSessionId = providerSessionId;
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
    }

    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }

    public String getProviderSessionId() { return providerSessionId; }
    public void setProviderSessionId(String providerSessionId) { this.providerSessionId = providerSessionId; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
