package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ReconciliationAssignRequest {
    @NotNull
    @Positive
    private Long assigneeAccountId;
    public Long getAssigneeAccountId() { return assigneeAccountId; }
    public void setAssigneeAccountId(Long value) { this.assigneeAccountId = value; }
}
