package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReconciliationResolveRequest {
    @NotBlank
    @Size(max = 100)
    private String resolutionCode;
    @NotBlank
    @Size(max = 2000)
    private String note;
    private boolean ignored;

    public String getResolutionCode() { return resolutionCode; }
    public void setResolutionCode(String value) { this.resolutionCode = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
    public boolean isIgnored() { return ignored; }
    public void setIgnored(boolean value) { this.ignored = value; }
}
