package com.project.paymentservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "payment.runtime")
@Validated
public class PaymentRuntimeProperties {
    @NotBlank
    private String frontendReturnUrl;
    @Min(1)
    private int idempotencyLeaseSeconds = 30;
    @Min(60)
    private int idempotencyTtlSeconds = 3600;
    @Min(1)
    private int settlementHoldSeconds = 60;
    @Min(1)
    private int outboxBatchSize = 20;
    @Min(1)
    private int outboxLeaseSeconds = 30;
    @Min(1)
    private int outboxMaxAttempts = 8;
    @Min(100)
    private long outboxFixedDelayMillis = 2000;
    @Min(100)
    private long providerRecoveryFixedDelayMillis = 5000;
    @Min(100)
    private long expiryFixedDelayMillis = 5000;
    @Min(1)
    private int refundBatchSize = 10;
    @Min(1)
    private int refundLeaseSeconds = 45;
    @Min(1)
    private int refundMaxAttempts = 8;
    @Min(100)
    private long refundFixedDelayMillis = 3000;
    @Min(15)
    private int refundProcessingPollSeconds = 60;
    @Min(1)
    private int refundProcessingMaxAgeHours = 72;
    @NotBlank
    private String analyticsTopic = "payment-success.v1";

    public String getFrontendReturnUrl() { return frontendReturnUrl; }
    public void setFrontendReturnUrl(String value) { this.frontendReturnUrl = value; }
    public int getIdempotencyLeaseSeconds() { return idempotencyLeaseSeconds; }
    public void setIdempotencyLeaseSeconds(int value) { this.idempotencyLeaseSeconds = value; }
    public int getIdempotencyTtlSeconds() { return idempotencyTtlSeconds; }
    public void setIdempotencyTtlSeconds(int value) { this.idempotencyTtlSeconds = value; }
    public int getSettlementHoldSeconds() { return settlementHoldSeconds; }
    public void setSettlementHoldSeconds(int value) { this.settlementHoldSeconds = value; }
    public int getOutboxBatchSize() { return outboxBatchSize; }
    public void setOutboxBatchSize(int value) { this.outboxBatchSize = value; }
    public int getOutboxLeaseSeconds() { return outboxLeaseSeconds; }
    public void setOutboxLeaseSeconds(int value) { this.outboxLeaseSeconds = value; }
    public int getOutboxMaxAttempts() { return outboxMaxAttempts; }
    public void setOutboxMaxAttempts(int value) { this.outboxMaxAttempts = value; }
    public long getOutboxFixedDelayMillis() { return outboxFixedDelayMillis; }
    public void setOutboxFixedDelayMillis(long value) { this.outboxFixedDelayMillis = value; }
    public long getProviderRecoveryFixedDelayMillis() { return providerRecoveryFixedDelayMillis; }
    public void setProviderRecoveryFixedDelayMillis(long value) { this.providerRecoveryFixedDelayMillis = value; }
    public long getExpiryFixedDelayMillis() { return expiryFixedDelayMillis; }
    public void setExpiryFixedDelayMillis(long value) { this.expiryFixedDelayMillis = value; }
    public int getRefundBatchSize() { return refundBatchSize; }
    public void setRefundBatchSize(int value) { this.refundBatchSize = value; }
    public int getRefundLeaseSeconds() { return refundLeaseSeconds; }
    public void setRefundLeaseSeconds(int value) { this.refundLeaseSeconds = value; }
    public int getRefundMaxAttempts() { return refundMaxAttempts; }
    public void setRefundMaxAttempts(int value) { this.refundMaxAttempts = value; }
    public long getRefundFixedDelayMillis() { return refundFixedDelayMillis; }
    public void setRefundFixedDelayMillis(long value) { this.refundFixedDelayMillis = value; }
    public int getRefundProcessingPollSeconds() { return refundProcessingPollSeconds; }
    public void setRefundProcessingPollSeconds(int value) {
        this.refundProcessingPollSeconds = value;
    }
    public int getRefundProcessingMaxAgeHours() { return refundProcessingMaxAgeHours; }
    public void setRefundProcessingMaxAgeHours(int value) {
        this.refundProcessingMaxAgeHours = value;
    }
    public String getAnalyticsTopic() { return analyticsTopic; }
    public void setAnalyticsTopic(String value) { this.analyticsTopic = value; }
}
