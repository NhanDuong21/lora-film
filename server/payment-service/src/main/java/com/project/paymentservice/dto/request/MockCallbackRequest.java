package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

public class MockCallbackRequest {

    @Schema(description = "ID of the payment to callback for", example = "1")
    @NotNull(message = "paymentId is required")
    private Long paymentId;

    @Schema(description = "Status to simulate", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
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
