package com.lorafilm.booking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class BookingPaymentResultRequestDto {
    private String eventId;
    private String schemaVersion;
    private Long paymentId;
    private String paymentTransactionCode;
    private String paymentMethod;
    private String result;
    private BigDecimal amount;
    private String currency;
    private Instant occurredAt;
    private String externalTransactionId;
    private String reconciliationStatus;

    public BookingPaymentResultRequestDto() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getPaymentTransactionCode() { return paymentTransactionCode; }
    public void setPaymentTransactionCode(String paymentTransactionCode) { this.paymentTransactionCode = paymentTransactionCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getExternalTransactionId() { return externalTransactionId; }
    public void setExternalTransactionId(String externalTransactionId) { this.externalTransactionId = externalTransactionId; }

    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
}
