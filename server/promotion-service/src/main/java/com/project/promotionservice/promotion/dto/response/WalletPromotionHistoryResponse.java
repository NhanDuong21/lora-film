package com.project.promotionservice.promotion.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletPromotionHistoryResponse(
        String eventType,
        Instant eventAt,
        String walletPublicId,
        String promotionPublicId,
        String promotionName,
        String promotionCode,
        BigDecimal discountAmount,
        String bookingPublicId,
        String bookingReference,
        String detail) {
}
