package com.project.promotionservice.promotion.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertTrue;
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
        @Size(max = 1) List<@Pattern(regexp = UUID_PATTERN) String> selectedUserPromotionPublicIds,
        @Size(max = 1) List<@Pattern(regexp = UUID_PATTERN) String> selectedPromotionPublicIds,
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]*$") String couponCode,
        @Size(max = 20) String customerPhone,
        @Pattern(regexp = UUID_PATTERN) String bookingPublicId,
        @Pattern(regexp = UUID_PATTERN) String orderPublicId,
        @Size(max = 10) String currency,
        JsonNode contextJson,
        @Min(60) @Max(1800) Integer holdDurationSeconds,
        @Size(max = 300) List<@Pattern(regexp = UUID_PATTERN) String> evaluationUserPromotionPublicIds,
        @Size(max = 300) List<@Pattern(regexp = UUID_PATTERN) String> evaluationPromotionPublicIds) {

    public PromotionCheckoutRequest(
            String userPublicId,
            BigDecimal originalAmount,
            List<String> selectedUserPromotionPublicIds,
            List<String> selectedPromotionPublicIds,
            String couponCode,
            String customerPhone,
            String bookingPublicId,
            String orderPublicId,
            String currency,
            JsonNode contextJson,
            Integer holdDurationSeconds) {
        this(userPublicId, originalAmount, selectedUserPromotionPublicIds,
                selectedPromotionPublicIds, couponCode, customerPhone,
                bookingPublicId, orderPublicId, currency, contextJson,
                holdDurationSeconds, List.of(), List.of());
    }

    @AssertTrue(message = "Only one voucher or coupon can be selected per booking")
    public boolean isSingleManualSelection() {
        return size(selectedUserPromotionPublicIds)
                + size(selectedPromotionPublicIds)
                + (couponCode == null || couponCode.isBlank() ? 0 : 1) <= 1;
    }

    private static int size(List<String> values) {
        return values == null ? 0 : (int) values.stream().distinct().count();
    }
}
