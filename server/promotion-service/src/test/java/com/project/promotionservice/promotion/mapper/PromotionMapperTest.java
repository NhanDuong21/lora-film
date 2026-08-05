package com.project.promotionservice.promotion.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.dto.response.PromotionResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.PromotionStackingBlockedReason;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionMapperTest {

    private final PromotionMapper mapper = new PromotionMapper(new ObjectMapper());

    @Test
    void responseSeparatesPromotionConfigurationFromCampaignEffect() {
        Promotion promotion = promotion(true);
        PromotionCampaign campaign = campaign(false);

        PromotionResponse response = mapper.response(promotion, campaign);

        assertThat(response.stackable()).isTrue();
        assertThat(response.campaignStackable()).isFalse();
        assertThat(response.effectiveStackable()).isFalse();
        assertThat(response.stackingBlockedReason())
                .isEqualTo(PromotionStackingBlockedReason.CAMPAIGN_STACKING_DISABLED);
    }

    @Test
    void responseIsEffectivelyStackableWhenBothLevelsAllowIt() {
        PromotionResponse response = mapper.response(
                promotion(true), campaign(true));

        assertThat(response.effectiveStackable()).isTrue();
        assertThat(response.stackingBlockedReason()).isNull();
    }

    @Test
    void responseExplainsWhenPromotionConfigurationDisablesStacking() {
        PromotionResponse response = mapper.response(
                promotion(false), campaign(true));

        assertThat(response.effectiveStackable()).isFalse();
        assertThat(response.stackingBlockedReason())
                .isEqualTo(PromotionStackingBlockedReason.PROMOTION_STACKING_DISABLED);
    }

    private Promotion promotion(boolean stackable) {
        Instant now = Instant.parse("2026-08-04T00:00:00Z");
        Promotion promotion = new Promotion();
        promotion.setPublicId("promotion-1");
        promotion.setCampaignPublicId("campaign-1");
        promotion.setPromotionType(PromotionType.VOUCHER);
        promotion.setCode("VOUCHER-1");
        promotion.setName("Voucher");
        promotion.setStatus(PromotionStatus.DRAFT);
        promotion.setPublicVisible(false);
        promotion.setPriority(100);
        promotion.setStackable(stackable);
        promotion.setConditionsJson("{}");
        promotion.setActionsJson("{}");
        promotion.setMaxRedemptionsPerUser(1);
        promotion.setValidFrom(now);
        promotion.setValidTo(now.plusSeconds(3600));
        return promotion;
    }

    private PromotionCampaign campaign(boolean stackable) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("campaign-1");
        campaign.setStackable(stackable);
        return campaign;
    }
}
