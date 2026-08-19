package com.project.promotionservice.promotion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ForceReleaseRequest(
        @NotBlank @Size(max = 100) String campaignCode,
        @NotBlank @Size(max = 1000) String reason,
        @NotBlank @Size(max = 64) String impactToken,
        @NotNull @PositiveOrZero Integer campaignVersion) {
}
