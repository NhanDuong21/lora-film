package com.project.promotionservice.promotion.dto.response;

import java.math.BigDecimal;

public record ForceReleaseImpactResponse(
        String campaignPublicId,
        long affectedReservationCount,
        long affectedBookingCount,
        BigDecimal reservedDiscount,
        BigDecimal budgetExposure,
        long bookingsRequiringRepriceOrCancel,
        int releasedCount) {
}
