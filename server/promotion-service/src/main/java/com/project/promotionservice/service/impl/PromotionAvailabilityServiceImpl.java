package com.project.promotionservice.service.impl;

import com.project.promotionservice.entity.Promotion;
import com.project.promotionservice.entity.PromotionCampaign;
import com.project.promotionservice.service.PromotionAvailabilityService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class PromotionAvailabilityServiceImpl implements PromotionAvailabilityService {

    @Override
    public String getCampaignStatus(PromotionCampaign campaign, LocalDateTime now) {
        if (!campaign.isActive()) {
            return "DISABLED";
        }
        if (now.isBefore(campaign.getStartDate())) {
            return "UPCOMING";
        }
        if (now.isAfter(campaign.getEndDate())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    @Override
    public String getPromotionStatus(Promotion promotion, LocalDateTime now) {
        PromotionCampaign campaign = promotion.getCampaign();
        if (!promotion.isActive() || (campaign != null && !campaign.isActive())) {
            return "DISABLED";
        }
        if (now.isBefore(promotion.getStartDate()) || (campaign != null && now.isBefore(campaign.getStartDate()))) {
            return "UPCOMING";
        }
        if (now.isAfter(promotion.getEndDate()) || (campaign != null && now.isAfter(campaign.getEndDate()))) {
            return "EXPIRED";
        }
        if (promotion.getUsedCount() >= promotion.getUsageLimit()) {
            return "OUT_OF_USAGE";
        }
        return "ACTIVE";
    }
}
