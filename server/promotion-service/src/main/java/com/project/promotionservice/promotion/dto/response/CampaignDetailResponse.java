package com.project.promotionservice.promotion.dto.response;

import java.util.List;

public class CampaignDetailResponse extends CampaignResponse {

    private List<RuleResponse> rules;

    public CampaignDetailResponse() {
        super();
    }

    public List<RuleResponse> getRules() {
        return rules;
    }

    public void setRules(List<RuleResponse> rules) {
        this.rules = rules;
    }
}
