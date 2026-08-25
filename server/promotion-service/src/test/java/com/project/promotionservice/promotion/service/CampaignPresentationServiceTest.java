package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.presentation.storage.PromotionAssetStorage;
import com.project.promotionservice.promotion.dto.request.CampaignPresentationUpdateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignPresentationResponse;
import com.project.promotionservice.promotion.entity.CampaignPresentation;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignPresentationStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.mapper.PromotionMapper;
import com.project.promotionservice.promotion.repository.CampaignPresentationRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignPresentationServiceTest {

    @Mock
    private CampaignPresentationRepository presentationRepository;
    @Mock
    private PromotionCampaignRepository campaignRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PromotionAssetStorage assetStorage;

    private CampaignPresentationService service;

    @BeforeEach
    void setUp() {
        service = new CampaignPresentationService(
                presentationRepository,
                campaignRepository,
                promotionRepository,
                new PromotionMapper(new ObjectMapper()),
                assetStorage);
    }

    @Test
    void returnsAnUnpersistedDraftUsingCampaignCopy() {
        PromotionCampaign campaign = campaign(CampaignStatus.DRAFT);
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.empty());

        CampaignPresentationResponse result = service.get("campaign-1");

        assertThat(result.publicId()).isNull();
        assertThat(result.status()).isEqualTo(CampaignPresentationStatus.DRAFT);
        assertThat(result.headline()).isEqualTo("Monday Movie Deal");
        assertThat(result.summary()).isEqualTo("Save more every Monday");
    }

    @Test
    void contentEditMovesPublishedPresentationBackToDraft() {
        PromotionCampaign campaign = campaign(CampaignStatus.ACTIVE);
        CampaignPresentation presentation = presentation();
        presentation.setStatus(CampaignPresentationStatus.PUBLISHED);
        presentation.setPublishedAt(Instant.now());
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(presentation));
        when(presentationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CampaignPresentationResponse result = service.update(
                "campaign-1",
                new CampaignPresentationUpdateRequest(
                        "Monday tickets from 60K",
                        "A better Monday at LoraFilm",
                        "A cinema audience enjoying Monday movies",
                        true,
                        10,
                        true,
                        true,
                        false,
                        null),
                "author-1");

        assertThat(result.status()).isEqualTo(CampaignPresentationStatus.DRAFT);
        assertThat(result.publishedAt()).isNull();
        assertThat(result.showOnHome()).isTrue();
    }

    @Test
    void publishRejectsMissingCoverAndAltText() {
        PromotionCampaign campaign = campaign(CampaignStatus.ACTIVE);
        CampaignPresentation presentation = presentation();
        presentation.setShowOnHome(true);
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(presentation));

        assertThatThrownBy(() -> service.publish("campaign-1", "publisher-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cover image");
    }

    @Test
    void publishRequiresApprovedActiveBusinessCampaign() {
        PromotionCampaign campaign = campaign(CampaignStatus.DRAFT);
        campaign.setApprovalStatus(CampaignApprovalStatus.DRAFT);
        CampaignPresentation presentation = presentation();
        presentation.setCoverImageUrl("/api/promotions/assets/cover.jpg");
        presentation.setImageAltText("Cinema seats");
        presentation.setShowOnHome(true);
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(presentation));

        assertThatThrownBy(() -> service.publish("campaign-1", "publisher-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("scheduled or active");
    }

    @Test
    void coverUploadStoresReferenceOutsideTheCampaignConfiguration() {
        PromotionCampaign campaign = campaign(CampaignStatus.ACTIVE);
        CampaignPresentation presentation = presentation();
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[]{1, 2, 3});
        when(campaignRepository.findByPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(campaign));
        when(presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull("campaign-1"))
                .thenReturn(Optional.of(presentation));
        when(assetStorage.storeCover(file)).thenReturn(
                new PromotionAssetStorage.StoredAsset(
                        "asset.png", "/api/promotions/assets/asset.png",
                        "image/png", 3));
        when(assetStorage.provider()).thenReturn("LOCAL");
        when(presentationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        CampaignPresentationResponse result = service.uploadCover(
                "campaign-1", file, "author-1");

        assertThat(result.coverImageUrl())
                .isEqualTo("/api/promotions/assets/asset.png");
        assertThat(result.coverImageStorageProvider()).isEqualTo("LOCAL");
        verify(assetStorage).deleteOnRollback("asset.png");
    }

    private PromotionCampaign campaign(CampaignStatus status) {
        PromotionCampaign campaign = new PromotionCampaign();
        campaign.setPublicId("campaign-1");
        campaign.setName("Monday Movie Deal");
        campaign.setSlug("monday-movie-deal");
        campaign.setDescription("Save more every Monday");
        campaign.setStatus(status);
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setLegalStatus(LegalStatus.PASSED);
        campaign.setTestData(false);
        campaign.setKillSwitch(false);
        campaign.setStartAt(Instant.now().minusSeconds(3600));
        campaign.setEndAt(Instant.now().plusSeconds(86400));
        return campaign;
    }

    private CampaignPresentation presentation() {
        CampaignPresentation presentation = new CampaignPresentation();
        presentation.setPublicId("presentation-1");
        presentation.setCampaignPublicId("campaign-1");
        presentation.setHeadline("Monday Movie Deal");
        presentation.setSummary("Save more every Monday");
        presentation.setStatus(CampaignPresentationStatus.DRAFT);
        return presentation;
    }
}
