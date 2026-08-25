package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.promotion.enums.CampaignPresentationStatus;
import java.time.Instant;

public record CampaignPresentationResponse(
        String publicId,
        Integer version,
        String campaignPublicId,
        CampaignPresentationStatus status,
        String headline,
        String summary,
        String coverImageUrl,
        String coverImageStorageProvider,
        String coverImageContentType,
        Long coverImageBytes,
        String imageAltText,
        boolean featured,
        int displayOrder,
        boolean showOnHome,
        boolean showInPromotionCenter,
        boolean showInWallet,
        String primaryPromotionPublicId,
        Instant publishedAt,
        String publishedBy,
        Instant updatedAt,
        String updatedBy) {
}
