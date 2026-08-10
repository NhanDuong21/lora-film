package com.project.promotionservice.promotion.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CampaignDetailResponse extends CampaignResponse {

    private List<PromotionResponse> promotions = new ArrayList<>();

    public List<PromotionResponse> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<PromotionResponse> promotions) {
        this.promotions = promotions == null ? new ArrayList<>() : promotions;
    }
}
