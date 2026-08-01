package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionEngineServiceTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private UserPromotionRepository walletRepository;
    @Mock
    private PromotionRedemptionRepository redemptionRepository;
    @Mock
    private PromotionCampaignRepository campaignRepository;

    private ObjectMapper objectMapper;
    private PromotionEngineService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new PromotionEngineService(
                promotionRepository,
                walletRepository,
                redemptionRepository,
                campaignRepository,
                new PromotionConditionEvaluator(),
                new PromotionDiscountCalculator(),
                objectMapper);
    }

    @Test
    void autoFullDiscountAppliesWithoutWalletOrCouponSelection() {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion promotion = activePromotion(
                "promotion-1", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FULL_DISCOUNT\"}");
        when(promotionRepository.findRuntimeCandidates(
                eq(PromotionType.AUTO), eq(PromotionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(promotion));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutResponse result = service.preview(request(new BigDecimal("285000")));

        assertThat(result.eligible()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("285000.00");
        assertThat(result.finalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.appliedPromotions()).singleElement().satisfies(applied -> {
            assertThat(applied.promotionType()).isEqualTo(PromotionType.AUTO);
            assertThat(applied.userPromotionPublicId()).isNull();
        });
    }

    @Test
    void ineligibleAutoPromotionBecomesAWarningAndDoesNotBreakCheckout() {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion promotion = activePromotion(
                "promotion-1", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setConditionsJson("{\"minimumOrderAmount\":500000}");
        when(promotionRepository.findRuntimeCandidates(
                eq(PromotionType.AUTO), eq(PromotionStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(promotion));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutResponse result = service.preview(request(new BigDecimal("285000")));

        assertThat(result.eligible()).isFalse();
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.warnings()).singleElement()
                .asString().contains("Minimum order amount is not met");
    }

    private PromotionCheckoutRequest request(BigDecimal amount) {
        return new PromotionCheckoutRequest(
                "1001", amount, List.of(), null, null,
                "11111111-1111-4111-8111-111111111111", null,
                "VND", objectMapper.createObjectNode(), 300);
    }

    private Promotion activePromotion(
            String publicId, String campaignId, PromotionType type, String actions) {
        Promotion promotion = new Promotion();
        promotion.setPublicId(publicId);
        promotion.setCampaignPublicId(campaignId);
        promotion.setPromotionType(type);
        promotion.setName("Free for everyone");
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setPriority(1);
        promotion.setStackable(false);
        promotion.setConditionsJson("{}");
        promotion.setActionsJson(actions);
        promotion.setMaxRedemptionsPerUser(1);
        promotion.setValidFrom(Instant.now().minusSeconds(60));
        promotion.setValidTo(Instant.now().plusSeconds(3600));
        return promotion;
    }

    private PromotionCampaign activeCampaign(String publicId) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId(publicId);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setLegalStatus(LegalStatus.PASSED);
        campaign.setKillSwitch(false);
        campaign.setStartAt(Instant.now().minusSeconds(60));
        campaign.setEndAt(Instant.now().plusSeconds(3600));
        campaign.setBudgetRemaining(new BigDecimal("10000000"));
        campaign.setBudgetReserved(BigDecimal.ZERO);
        campaign.setMaxRedemptionsPerUser(1);
        return campaign;
    }
}
