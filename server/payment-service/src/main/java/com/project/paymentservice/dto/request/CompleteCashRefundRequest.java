package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompleteCashRefundRequest {
    @NotBlank
    @Size(max = 150)
    private String providerReference;

    @NotBlank
    @Size(max = 1000)
    private String note;

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String value) {
        this.providerReference = value;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String value) {
        this.note = value;
    }
}
