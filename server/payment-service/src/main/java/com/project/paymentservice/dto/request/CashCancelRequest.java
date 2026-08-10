package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.Size;

public class CashCancelRequest {

    @Size(max = 500)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
