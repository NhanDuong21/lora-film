package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RefundDecisionRequest {
    @NotBlank
    @Size(min = 5, max = 1000)
    private String note;

    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
}
