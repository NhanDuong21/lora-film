package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.ForceReleaseDisposition;

public record ForceReleaseBookingImpact(
        String reservationPublicId,
        String bookingPublicId,
        String bookingStatus,
        ForceReleaseDisposition disposition,
        String reason) {
}
