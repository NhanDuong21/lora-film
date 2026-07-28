package com.project.paymentservice.provider;

import java.math.BigDecimal;
import java.time.Instant;

public class ProviderCallbackResult {
    private boolean signatureValid;
    private String deduplicationKey;
    private String providerOrderId;
    private String externalTransactionId;
    private String result;
    private String responseCode;
    private BigDecimal amount;
    private String currency;
    private String eventType;
    private Instant occurredAt;
    private String sanitizedPayload;

    public ProviderCallbackResult() {
    }
    public boolean isSignatureValid() { return signatureValid; }
    public void setSignatureValid(boolean value) { this.signatureValid = value; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public void setDeduplicationKey(String value) { this.deduplicationKey = value; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String value) { this.providerOrderId = value; }
    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String value) { this.externalTransactionId = value; }
    public String getResult() { return result; }
    public void setResult(String value) { this.result = value; }
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String value) { this.responseCode = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public String getEventType() { return eventType; }
    public void setEventType(String value) { this.eventType = value; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }
    public String getSanitizedPayload() { return sanitizedPayload; }
    public void setSanitizedPayload(String value) { this.sanitizedPayload = value; }
}
