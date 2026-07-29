package com.project.promotionservice.partner.dto.request;

import com.project.promotionservice.partner.enums.SettlementStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class SettlementUpdateRequest {

    private SettlementStatus status;

    @DecimalMin("0.00")
    private BigDecimal adjustmentAmount;

    @Size(max = 1000)
    private String note;

    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public BigDecimal getAdjustmentAmount() { return adjustmentAmount; }
    public void setAdjustmentAmount(BigDecimal adjustmentAmount) { this.adjustmentAmount = adjustmentAmount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
