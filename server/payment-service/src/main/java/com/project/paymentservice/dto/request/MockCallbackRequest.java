package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class MockCallbackRequest {

    @Schema(description = "Deprecated numeric payment ID; the canonical route identifies the payment")
    private Long paymentId;

    @Schema(description = "Status to simulate", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
    @NotBlank(message = "Trạng thái giả lập không được để trống")
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
