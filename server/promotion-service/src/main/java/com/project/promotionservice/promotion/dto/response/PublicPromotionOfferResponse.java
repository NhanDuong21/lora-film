package com.project.promotionservice.promotion.dto.response;

import java.time.Instant;

public record PublicPromotionOfferResponse(
        String campaignPublicId,
        String slug,
        String headline,
        String summary,
        String coverImageUrl,
        String imageAltText,
        boolean featured,
        int displayOrder,
        Instant validFrom,
        Instant validTo,
        PromotionResponse primaryPromotion) {
}
