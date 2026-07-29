package com.project.paymentservice.dto.request;

import com.project.paymentservice.enumtype.RefundComponent;
import com.project.paymentservice.enumtype.RefundType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateRefundRequest {
    @NotNull
    private RefundType refundType;
    @NotNull
    private RefundComponent refundComponent;
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal amount;
    @NotBlank
    @Size(max = 100)
    private String reasonCode;
    @NotBlank
    @Size(max = 2000)
    private String note;

    public RefundType getRefundType() { return refundType; }
    public void setRefundType(RefundType value) { this.refundType = value; }
    public RefundComponent getRefundComponent() { return refundComponent; }
    public void setRefundComponent(RefundComponent value) { this.refundComponent = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { this.amount = value; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String value) { this.reasonCode = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
}
