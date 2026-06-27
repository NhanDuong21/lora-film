package com.project.promotionservice.dto;

import com.project.promotionservice.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DiscountConstraint
public class UpdatePromotionRequest {

    @NotBlank(message = "promotionCode is required")
    @Size(max = 50, message = "promotionCode cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "promotionCode can only contain alphanumeric characters, underscores, and hyphens in uppercase")
    private String promotionCode;

    private String description;

    @NotNull(message = "discountType is required")
    private DiscountType discountType;

    @NotNull(message = "discountValue is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "discountValue must be greater than 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "maxDiscountAmount must be greater than or equal to 0")
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "minOrderAmount is required")
    @DecimalMin(value = "0.0", message = "minOrderAmount must be greater than or equal to 0")
    private BigDecimal minOrderAmount;

    @NotNull(message = "usageLimit is required")
    @Min(value = 1, message = "usageLimit must be at least 1")
    private Integer usageLimit;

    @NotNull(message = "limitPerUser is required")
    @Min(value = 1, message = "limitPerUser must be at least 1")
    private Integer limitPerUser;

    @NotNull(message = "startDate is required")
    private LocalDateTime startDate;

    @NotNull(message = "endDate is required")
    private LocalDateTime endDate;

    @NotNull(message = "campaignId is required")
    @Min(value = 1, message = "campaignId must be greater than 0")
    private Long campaignId;

    @Builder.Default
    private Boolean isActive = true;
}
