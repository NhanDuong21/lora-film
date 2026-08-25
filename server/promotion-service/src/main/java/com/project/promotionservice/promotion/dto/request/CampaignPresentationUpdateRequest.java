package com.project.promotionservice.promotion.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CampaignPresentationUpdateRequest(
        @NotBlank @Size(max = 180) String headline,
        @NotBlank @Size(max = 500) String summary,
        @Size(max = 240) String imageAltText,
        Boolean featured,
        @Min(0) @Max(10_000) Integer displayOrder,
        Boolean showOnHome,
        Boolean showInPromotionCenter,
        Boolean showInWallet,
        @Size(max = 36) String primaryPromotionPublicId) {
}
