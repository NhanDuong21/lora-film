package com.project.promotionservice.common.monitoring;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PromotionOperationsSearchResponse(
        List<LedgerItem> reservations,
        List<LedgerItem> redemptions,
        List<LedgerItem> adjustments,
        long reservationTotal,
        long redemptionTotal,
        long adjustmentTotal) {

    public record LedgerItem(
            String entryType,
            String publicId,
            String businessReference,
            String status,
            String campaignPublicId,
            String promotionPublicId,
            String reservationPublicId,
            String bookingPublicId,
            String orderPublicId,
            String paymentPublicId,
            String customerReference,
            String releaseReasonType,
            String reasonDetail,
            String sourceReference,
            BigDecimal discountAmount,
            Instant occurredAt,
            Boolean testData,
            String environmentTag) {
    }
}
