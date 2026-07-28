package com.project.promotionservice.promotion.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for submitting, approving or rejecting")
public class ApprovalRequest {

    @Size(max = 1000, message = "comment must be at most 1000 characters")
    private String comment;

    public ApprovalRequest() {
    }

    public ApprovalRequest(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
