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
public class PromotionUsageResponse {
    private Long usageId;
    private Long promotionId;
    private String promotionCode;
    private Long bookingId;
    private Long userId;
    private String status;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime reservedAt;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private LocalDateTime confirmedAt;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private LocalDateTime revertedAt;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String revertReason;

    private Boolean idempotent;
}
