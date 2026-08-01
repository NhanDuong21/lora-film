package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.dto.request.PromotionCheckoutRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCheckoutResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    void systemPromotionAppliesWhenSelectedByPromotionPublicId() {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion promotion = activePromotion(
                "promotion-1", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FULL_DISCOUNT\"}");
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("promotion-1"))
                .thenReturn(Optional.of(promotion));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutResponse result = service.preview(request(
                new BigDecimal("285000"), List.of("promotion-1")));

        assertThat(result.eligible()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("285000.00");
        assertThat(result.finalAmount()).isEqualByComparingTo("0.00");
        assertThat(result.appliedPromotions()).singleElement().satisfies(applied -> {
            assertThat(applied.promotionType()).isEqualTo(PromotionType.AUTO);
            assertThat(applied.userPromotionPublicId()).isNull();
        });
    }

    @Test
    void systemPromotionDoesNotApplyWhenCustomerDidNotSelectIt() {
        PromotionCheckoutResponse result = service.preview(request(new BigDecimal("285000")));

        assertThat(result.eligible()).isFalse();
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
        assertThat(result.appliedPromotions()).isEmpty();
    }

    @Test
    void automaticallyAppliesTheBestEligibleSystemPromotion() {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion lower = activePromotion(
                "promotion-low", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":20000}");
        Promotion higher = activePromotion(
                "promotion-high", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        when(promotionRepository.findRuntimeCandidates(
                eq(PromotionType.AUTO), eq(PromotionStatus.ACTIVE), any()))
                .thenReturn(List.of(lower, higher));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutResponse result = service.preview(request(new BigDecimal("285000")));

        assertThat(result.appliedPromotions()).singleElement()
                .extracting(applied -> applied.promotionPublicId())
                .isEqualTo("promotion-high");
        assertThat(result.discountAmount()).isEqualByComparingTo("50000.00");
    }

    @Test
    void explicitSystemPromotionOverridesTheAutomaticBestChoice() {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion lower = activePromotion(
                "promotion-low", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":20000}");
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("promotion-low"))
                .thenReturn(Optional.of(lower));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutResponse result = service.preview(request(
                new BigDecimal("285000"), List.of("promotion-low")));

        assertThat(result.appliedPromotions()).singleElement()
                .extracting(applied -> applied.promotionPublicId())
                .isEqualTo("promotion-low");
        assertThat(result.discountAmount()).isEqualByComparingTo("20000.00");
    }

    @Test
    void returnsAuthoritativeEligibilityForAConfiguredMovie() throws Exception {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion promotion = activePromotion(
                "promotion-1", "campaign-1", PromotionType.VOUCHER,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setConditionsJson("{\"moviePublicIds\":[\"movie-other\"]}");
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("promotion-1"))
                .thenReturn(Optional.of(promotion));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutRequest request = new PromotionCheckoutRequest(
                "1001", new BigDecimal("285000"), List.of(), List.of(), null, null,
                "11111111-1111-4111-8111-111111111111", null, "VND",
                objectMapper.readTree("{\"moviePublicId\":\"movie-current\"}"), 300,
                List.of(), List.of("promotion-1"));
        PromotionCheckoutResponse result = service.preview(request);

        assertThat(result.promotionEvaluations()).singleElement().satisfies(evaluation -> {
            assertThat(evaluation.eligible()).isFalse();
            assertThat(evaluation.reasonCode()).isEqualTo("MOVIE_NOT_APPLICABLE");
            assertThat(evaluation.reason()).contains("phim hiện tại");
        });
    }

    @Test
    void returnsAuthoritativeEligibilityForPaymentMethodRules() throws Exception {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        Promotion promotion = activePromotion(
                "promotion-2", "campaign-1", PromotionType.VOUCHER,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setConditionsJson("{\"paymentMethods\":[\"MOMO\"]}");
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("promotion-2"))
                .thenReturn(Optional.of(promotion));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutRequest request = new PromotionCheckoutRequest(
                "1001", new BigDecimal("285000"), List.of(), List.of(), null, null,
                "11111111-1111-4111-8111-111111111111", null, "VND",
                objectMapper.readTree("{\"paymentMethod\":\"VNPAY\"}"), 300,
                List.of(), List.of("promotion-2"));
        PromotionCheckoutResponse result = service.preview(request);

        assertThat(result.promotionEvaluations()).singleElement().satisfies(evaluation -> {
            assertThat(evaluation.eligible()).isFalse();
            assertThat(evaluation.reasonCode()).isEqualTo("PAYMENT_METHOD_NOT_APPLICABLE");
            assertThat(evaluation.reason()).contains("phương thức thanh toán");
        });
    }

    @Test
    void campaignUsageLimitsAreScopedByPromotionType() {
        PromotionCampaign campaign = activeCampaign("campaign-1");
        campaign.setMaxRedemptions(1);
        campaign.setMaxRedemptionsPerUser(1);
        Promotion voucher = activePromotion(
                "voucher-1", "campaign-1", PromotionType.VOUCHER,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":20000}");
        Promotion system = activePromotion(
                "auto-1", "campaign-1", PromotionType.AUTO,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":30000}");
        UserPromotion wallet = walletItem("wallet-voucher", "1001", "voucher-1");
        when(walletRepository.findByPublicIdAndDeletedAtIsNull("wallet-voucher"))
                .thenReturn(Optional.of(wallet));
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("voucher-1"))
                .thenReturn(Optional.of(voucher));
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("auto-1"))
                .thenReturn(Optional.of(system));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCheckoutRequest request = new PromotionCheckoutRequest(
                "1001", new BigDecimal("285000"), List.of(), List.of(), null, null,
                "11111111-1111-4111-8111-111111111111", null,
                "VND", objectMapper.createObjectNode(), 300,
                List.of("wallet-voucher"), List.of("auto-1"));
        PromotionCheckoutResponse result = service.preview(request);

        assertThat(result.promotionEvaluations())
                .extracting(evaluation -> evaluation.promotionType())
                .containsExactlyInAnyOrder(PromotionType.AUTO, PromotionType.VOUCHER);
        assertThat(result.promotionEvaluations())
                .allSatisfy(evaluation -> assertThat(evaluation.eligible()).isTrue());
        verify(redemptionRepository).countCampaignUserRedemptionsByPromotionType(
                eq("campaign-1"), eq(PromotionType.VOUCHER), eq("1001"), any());
        verify(redemptionRepository).countCampaignUserRedemptionsByPromotionType(
                eq("campaign-1"), eq(PromotionType.AUTO), eq("1001"), any());
    }

    @Test
    void couponMustBeIssuedToTheCustomerBeforeCheckoutCanUseItsCode() {
        Promotion coupon = activePromotion(
                "coupon-1", "campaign-1", PromotionType.COUPON,
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        coupon.setCode("CPN-PRIVATE");
        when(promotionRepository.findByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
                PromotionType.COUPON, "CPN-PRIVATE"))
                .thenReturn(Optional.of(coupon));
        when(walletRepository.findByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
                "1001", "coupon-1"))
                .thenReturn(Optional.empty());

        PromotionCheckoutRequest request = new PromotionCheckoutRequest(
                "1001", new BigDecimal("285000"), List.of(), List.of(), "CPN-PRIVATE", null,
                "11111111-1111-4111-8111-111111111111", null,
                "VND", objectMapper.createObjectNode(), 300);

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Coupon was not issued to this customer");
    }

    private PromotionCheckoutRequest request(BigDecimal amount) {
        return request(amount, List.of());
    }

    private PromotionCheckoutRequest request(BigDecimal amount, List<String> selectedPromotionIds) {
        return new PromotionCheckoutRequest(
                "1001", amount, List.of(), selectedPromotionIds, null, null,
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

    private UserPromotion walletItem(
            String publicId, String userPublicId, String promotionPublicId) {
        UserPromotion wallet = new UserPromotion();
        wallet.setPublicId(publicId);
        wallet.setUserPublicId(userPublicId);
        wallet.setPromotionPublicId(promotionPublicId);
        wallet.setStatus(UserPromotionStatus.AVAILABLE);
        wallet.setUsageCount(0);
        wallet.setMaxUsage(1);
        wallet.setClaimedAt(Instant.now().minusSeconds(60));
        wallet.setValidFrom(Instant.now().minusSeconds(60));
        wallet.setValidTo(Instant.now().plusSeconds(3600));
        return wallet;
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
