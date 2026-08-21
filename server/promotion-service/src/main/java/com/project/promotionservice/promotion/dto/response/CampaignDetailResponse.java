package com.project.promotionservice.promotion.dto.response;

import com.project.promotionservice.automation.dto.AutomationDtos.CampaignAutomationView;

import java.util.ArrayList;
import java.util.List;

public class CampaignDetailResponse extends CampaignResponse {

    private List<PromotionResponse> promotions = new ArrayList<>();
    private CampaignAutomationView automation;

    public List<PromotionResponse> getPromotions() {
        return promotions;
    }

    public void setPromotions(List<PromotionResponse> promotions) {
        this.promotions = promotions == null ? new ArrayList<>() : promotions;
    }

    public CampaignAutomationView getAutomation() { return automation; }
    public void setAutomation(CampaignAutomationView value) { automation = value; }
}
