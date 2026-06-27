package com.project.promotionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.promotionservice.enums.DiscountType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromotionResponse {
    private Long promotionId;
    private String promotionCode;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private Integer usageLimit;
    private Integer limitPerUser;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private String availabilityStatus;
    private Long campaignId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
