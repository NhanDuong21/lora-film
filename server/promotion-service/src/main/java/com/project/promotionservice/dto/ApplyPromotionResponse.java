package com.project.promotionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplyPromotionResponse {
    private Long usageId;
    private Long promotionId;
    private String promotionCode;
    private Long bookingId;
    private Long userId;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String usageStatus;
    private LocalDateTime expiresAt;
    private LocalDateTime reservedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime revertedAt;
    private String revertReason;
    private Boolean idempotent;
}
