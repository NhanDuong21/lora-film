package com.project.promotionservice.promotion.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

import static com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN;
import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

public record PromotionCheckoutRequest(
        @NotBlank
        @Pattern(regexp = USER_REFERENCE_PATTERN,
                message = "userPublicId must be a positive account ID or a valid UUID")
        String userPublicId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal originalAmount,
        @Size(max = 10) List<@Pattern(regexp = UUID_PATTERN) String> selectedUserPromotionPublicIds,
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$") String couponCode,
        @Size(max = 20) String customerPhone,
        @Pattern(regexp = UUID_PATTERN) String bookingPublicId,
        @Pattern(regexp = UUID_PATTERN) String orderPublicId,
        @Size(max = 10) String currency,
        JsonNode contextJson,
        @Min(60) @Max(1800) Integer holdDurationSeconds) {
}
