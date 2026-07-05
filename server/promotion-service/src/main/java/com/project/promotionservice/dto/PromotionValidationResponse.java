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
public class PromotionValidationResponse {
    private Boolean valid;
    private Long promotionId;
    private String promotionCode;
    private Long bookingId;
    private BigDecimal originalAmount;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime expiresAt;
    private String currency;
    private Boolean previewOnly;
}
