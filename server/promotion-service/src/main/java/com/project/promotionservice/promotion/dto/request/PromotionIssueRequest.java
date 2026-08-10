package com.project.promotionservice.promotion.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

import static com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN;

public record PromotionIssueRequest(
        @NotEmpty
        @Size(max = 1000)
        List<@Size(min = 1, max = 36)
        @Pattern(regexp = USER_REFERENCE_PATTERN) String> userPublicIds) {
}
