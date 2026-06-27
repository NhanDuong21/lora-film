package com.project.promotionservice.dto;

import com.project.promotionservice.enums.DiscountType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionListItemResponse {
    private Long promotionId;
    private String promotionCode;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer usageLimit;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private String availabilityStatus;
    private Long campaignId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
