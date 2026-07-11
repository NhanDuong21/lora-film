package com.project.paymentservice.dto.response;

public class PaymentStatusResponse {

    private Long paymentId;
    private String status;
    private String reconciliationStatus;

    public PaymentStatusResponse() {
    }

    public PaymentStatusResponse(Long paymentId, String status, String reconciliationStatus) {
        this.paymentId = paymentId;
        this.status = status;
        this.reconciliationStatus = reconciliationStatus;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
}
