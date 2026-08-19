package com.project.promotionservice.promotion.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ForceReleaseImpactResponse(
        String campaignPublicId,
        long affectedReservationCount,
        long affectedBookingCount,
        BigDecimal reservedDiscount,
        BigDecimal budgetExposure,
        long bookingsRequiringRepriceOrCancel,
        int releasedCount,
        Integer campaignVersion,
        String impactToken,
        Instant generatedAt,
        long safeToReleaseCount,
        long repriceRequiredCount,
        long blockedCount,
        boolean executable,
        List<ForceReleaseBookingImpact> bookings) {
}
