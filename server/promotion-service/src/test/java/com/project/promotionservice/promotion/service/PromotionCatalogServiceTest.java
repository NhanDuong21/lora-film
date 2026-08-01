package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.integration.client.UserRecipientValidationClient;
import com.project.promotionservice.promotion.dto.request.PromotionUpsertRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCloneDraftResponse;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.mapper.PromotionMapper;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    private PromotionRedemptionRepository redemptionRepository;
    private PromotionMapper mapper;
    @Mock
    private PromotionPolicyValidator policyValidator;
    private CampaignConfigurationPolicy campaignPolicy;
    @Mock
    private PromotionCatalogEventService eventService;
    @Mock
    private UserRecipientValidationClient recipientValidationClient;

    private PromotionCatalogService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new PromotionMapper(objectMapper);
        campaignPolicy = new CampaignConfigurationPolicy();
        service = new PromotionCatalogService(
                promotionRepository,
                walletRepository,
                campaignRepository,
                redemptionRepository,
                mapper,
                policyValidator,
                campaignPolicy,
                eventService,
                recipientValidationClient);
    }

    @Test
    void cloneDraftDoesNotPersistAndRequiresAnotherCampaignWhenSourceIsLocked() {
        Instant now = Instant.now();
        Promotion source = promotion(
                PromotionType.VOUCHER, "Summer voucher", "SUMMER",
                now.minusSeconds(3600), now.plusSeconds(86400));
        PromotionCampaign campaign = campaign(
                CampaignStatus.ACTIVE, CampaignApprovalStatus.APPROVED,
                now.minusSeconds(86400), now.plusSeconds(172800));
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("promotion-1"))
                .thenReturn(Optional.of(source));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        PromotionCloneDraftResponse result = service.buildCloneDraft("promotion-1");

        assertThat(result.sourceCampaignEditable()).isFalse();
        assertThat(result.suggestedCampaignPublicId()).isNull();
        assertThat(result.suggestedCode()).isNull();
        assertThat(result.publicVisible()).isTrue();
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void cloneDraftShiftsExpiredValidityAndIncrementsDuplicateName() {
        Instant before = Instant.now();
        Promotion source = promotion(
                PromotionType.COUPON, "Loyal customer", "CPN-OLD",
                before.minusSeconds(172800), before.minusSeconds(86400));
        PromotionCampaign campaign = campaign(
                CampaignStatus.DRAFT, CampaignApprovalStatus.REJECTED,
                before.minusSeconds(259200), before.plusSeconds(259200));
        when(promotionRepository.findByPublicIdAndDeletedAtIsNull("promotion-1"))
                .thenReturn(Optional.of(source));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(promotionRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(
                "Loyal customer (Copy)"))
                .thenReturn(true);
        when(promotionRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(
                "Loyal customer (Copy 2)"))
                .thenReturn(false);

        PromotionCloneDraftResponse result = service.buildCloneDraft("promotion-1");

        assertThat(result.sourceCampaignEditable()).isTrue();
        assertThat(result.suggestedCampaignPublicId()).isEqualTo("campaign-1");
        assertThat(result.suggestedName()).isEqualTo("Loyal customer (Copy 2)");
        assertThat(result.suggestedCode()).startsWith("CPN-");
        assertThat(result.validityWindowShifted()).isTrue();
        assertThat(result.suggestedValidFrom()).isAfterOrEqualTo(before);
        assertThat(result.suggestedValidTo()).isAfter(result.suggestedValidFrom());
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void createPersistsCloneLineageThroughTheNormalPipeline() {
        Instant now = Instant.now();
        PromotionCampaign campaign = campaign(
                CampaignStatus.DRAFT, CampaignApprovalStatus.DRAFT,
                now.minusSeconds(3600), now.plusSeconds(172800));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(promotionRepository.save(any(Promotion.class)))
                .thenAnswer(invocation -> {
                    Promotion saved = invocation.getArgument(0);
                    saved.setPublicId("promotion-copy");
                    return saved;
                });

        service.create(upsert("UNIQUE", now, "promotion-source"), "admin");

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        assertThat(captor.getValue().getClonedFromPublicId())
                .isEqualTo("promotion-source");
        verify(eventService).record(eq("PROMOTION"), eq("promotion-copy"),
                eq("PROMOTION_CLONED_FROM"),
                eq(Map.of("sourcePublicId", "promotion-source")), eq("admin"));
    }

    @Test
    void createCloneIsBlockedWhenTargetCampaignIsNotEditable() {
        Instant now = Instant.now();
        PromotionCampaign campaign = campaign(
                CampaignStatus.ACTIVE, CampaignApprovalStatus.APPROVED,
                now.minusSeconds(3600), now.plusSeconds(172800));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.create(
                upsert("UNIQUE", now, "promotion-source"), "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void createCloneRejectsDuplicateVoucherCode() {
        Instant now = Instant.now();
        PromotionCampaign campaign = campaign(
                CampaignStatus.DRAFT, CampaignApprovalStatus.DRAFT,
                now.minusSeconds(3600), now.plusSeconds(172800));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(promotionRepository
                .existsByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
                        PromotionType.VOUCHER, "DUPLICATE"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(
                upsert("DUPLICATE", now, "promotion-source"), "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("status")
                .isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
        verify(promotionRepository, never()).save(any(Promotion.class));
    }

    @Test
    void issueCouponCountsGrantedUsersButKeepsIssuedItemsOutOfWalletResponse() {
        Promotion coupon = couponPromotion();
        PromotionCampaign campaign = campaign(
                CampaignStatus.ACTIVE, CampaignApprovalStatus.APPROVED,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-09-01T00:00:00Z"));
        campaign.setLegalStatus(com.project.promotionservice.promotion.enums.LegalStatus.PASSED);
        campaign.setKillSwitch(false);
        when(promotionRepository.findByPublicIdForUpdate("coupon-1"))
                .thenReturn(Optional.of(coupon));
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(walletRepository.findFirstByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNullOrderByIdDesc(
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

    private Promotion promotion(
            PromotionType type, String name, String code,
            Instant validFrom, Instant validTo) {
        Promotion promotion = new Promotion();
        promotion.setPublicId("promotion-1");
        promotion.setCampaignPublicId("campaign-1");
        promotion.setPromotionType(type);
        promotion.setCode(code);
        promotion.setName(name);
        promotion.setDescription("Description");
        promotion.setStatus(PromotionStatus.DRAFT);
        promotion.setPublicVisible(type == PromotionType.VOUCHER);
        promotion.setPriority(100);
        promotion.setStackable(false);
        promotion.setConditionsJson("{}");
        promotion.setActionsJson(
                "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":50000}");
        promotion.setMetadataJson("{}");
        promotion.setMaxRedemptions(100);
        promotion.setMaxRedemptionsPerUser(1);
        promotion.setValidFrom(validFrom);
        promotion.setValidTo(validTo);
        return promotion;
    }

    private PromotionCampaign campaign(
            CampaignStatus status, CampaignApprovalStatus approvalStatus,
            Instant startAt, Instant endAt) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("campaign-1");
        campaign.setStatus(status);
        campaign.setApprovalStatus(approvalStatus);
        campaign.setStartAt(startAt);
        campaign.setEndAt(endAt);
        return campaign;
    }

    private PromotionUpsertRequest upsert(
            String code, Instant now, String clonedFromPublicId) {
        return new PromotionUpsertRequest(
                "campaign-1", PromotionType.VOUCHER, code, "Voucher copy",
                "Description", false, 100, false,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
                        .put("discountType", "FIXED_AMOUNT")
                        .put("discountValue", 50000),
                objectMapper.createObjectNode(), 100, 1,
                now, now.plusSeconds(86400), clonedFromPublicId);
    }
}
