package com.project.promotionservice.promotion.service;

import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.mapper.PromotionMapper;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionCatalogServiceTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private UserPromotionRepository walletRepository;
    @Mock
    private PromotionCampaignRepository campaignRepository;
    @Mock
    private PromotionMapper mapper;
    @Mock
    private PromotionPolicyValidator policyValidator;
    @Mock
    private CampaignConfigurationPolicy campaignPolicy;
    @Mock
    private PromotionCatalogEventService eventService;

    private PromotionCatalogService service;

    @BeforeEach
    void setUp() {
        service = new PromotionCatalogService(
                promotionRepository,
                walletRepository,
                campaignRepository,
                mapper,
                policyValidator,
                campaignPolicy,
                eventService);
    }

    @Test
    void issueCouponCountsGrantedUsersButKeepsIssuedItemsOutOfWalletResponse() {
        Promotion coupon = couponPromotion();
        when(promotionRepository.findByPublicIdForUpdate("coupon-1"))
                .thenReturn(Optional.of(coupon));
        when(walletRepository.existsByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
                "user-1", "coupon-1"))
                .thenReturn(false);
        when(walletRepository.findByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
                "user-1", "coupon-1"))
                .thenReturn(Optional.empty());
        when(walletRepository.save(org.mockito.ArgumentMatchers.any(UserPromotion.class)))
                .thenAnswer(invocation -> {
                    UserPromotion grant = invocation.getArgument(0);
                    grant.setPublicId("grant-1");
                    return grant;
                });

        PromotionIssueResponse result = service.issue(
                "coupon-1", List.of("user-1"), "admin");

        assertThat(result.issuedCount()).isEqualTo(1);
        assertThat(result.alreadyOwnedCount()).isZero();
        assertThat(result.issuedItems()).isEmpty();

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventService).record(eq("USER_PROMOTION"), eq("grant-1"),
                eq("COUPON_ISSUED"), payloadCaptor.capture(), eq("admin"));
        assertThat(payloadCaptor.getValue())
                .isInstanceOfSatisfying(Map.class, payload -> {
                    assertThat(payload).containsEntry("userPublicId", "user-1");
                    assertThat(payload).containsEntry("couponCode", "CPN-PRIVATE");
                    assertThat(payload).containsEntry("promotionName", "Private coupon");
                    assertThat(payload).containsEntry("deepLink", "/booking");
                });
    }

    private Promotion couponPromotion() {
        Promotion promotion = new Promotion();
        promotion.setPublicId("coupon-1");
        promotion.setCampaignPublicId("campaign-1");
        promotion.setPromotionType(PromotionType.COUPON);
        promotion.setCode("CPN-PRIVATE");
        promotion.setName("Private coupon");
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setValidFrom(Instant.parse("2026-08-01T00:00:00Z"));
        promotion.setValidTo(Instant.parse("2026-08-31T23:59:59Z"));
        promotion.setMaxRedemptionsPerUser(1);
        promotion.setActionsJson("{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setConditionsJson("{}");
        return promotion;
    }
}
