package com.project.paymentservice.dto.response;

import java.time.Instant;

public class CancelPaymentResponse {
    private Long paymentId;
    private String paymentPublicId;
    private String status;
    private Instant cancelledAt;
    public CancelPaymentResponse() {
    }
    public CancelPaymentResponse(Long paymentId, String status, Instant cancelledAt) {
        this.paymentId = paymentId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long value) { this.paymentId = value; }
    public String getPaymentPublicId() { return paymentPublicId; }
    public void setPaymentPublicId(String value) { this.paymentPublicId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant value) { this.cancelledAt = value; }
}
