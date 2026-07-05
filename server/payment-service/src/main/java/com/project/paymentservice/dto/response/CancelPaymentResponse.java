package com.project.paymentservice.dto.response;

import java.time.LocalDateTime;

public class CancelPaymentResponse {

    private Long paymentId;
    private String status;
    private LocalDateTime cancelledAt;

    public CancelPaymentResponse() {
    }

    public CancelPaymentResponse(Long paymentId, String status, LocalDateTime cancelledAt) {
        this.paymentId = paymentId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
