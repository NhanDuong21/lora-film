package com.project.promotionservice.promotion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApprovalOverrideRequest(
        @NotBlank @Size(max = 100) String campaignCode,
        @NotBlank @Size(max = 1000) String reason) {
}
