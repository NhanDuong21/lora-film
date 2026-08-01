package com.project.promotionservice.promotion.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PromotionIssueRequest(
        @NotEmpty
        @Size(max = 1000)
        List<@Size(min = 1, max = 36) String> userPublicIds) {
}
