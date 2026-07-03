package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;

public class MockCallbackRequest {

    @NotNull(message = "paymentId is required")
    private Long paymentId;

    @NotNull(message = "simulatedStatus is required")
    private String simulatedStatus;

    public MockCallbackRequest() {
    }

    public MockCallbackRequest(Long paymentId, String simulatedStatus) {
        this.paymentId = paymentId;
        this.simulatedStatus = simulatedStatus;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getSimulatedStatus() {
        return simulatedStatus;
    }

    public void setSimulatedStatus(String simulatedStatus) {
        this.simulatedStatus = simulatedStatus;
    }
}
