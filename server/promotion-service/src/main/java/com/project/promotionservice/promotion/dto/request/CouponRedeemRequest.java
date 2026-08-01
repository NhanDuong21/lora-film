package com.project.promotionservice.promotion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CouponRedeemRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$",
                message = "code must contain only letters, numbers, underscores, or hyphens")
        String code) {
}
