package com.project.promotionservice.promotion.dto.response;

import java.util.List;

public record PromotionIssueResponse(
        int issuedCount,
        int alreadyOwnedCount,
        List<WalletPromotionResponse> issuedItems) {
}
