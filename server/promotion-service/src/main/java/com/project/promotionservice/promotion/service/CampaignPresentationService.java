package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.presentation.storage.PromotionAssetStorage;
import com.project.promotionservice.promotion.dto.request.CampaignPresentationUpdateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignPresentationResponse;
import com.project.promotionservice.promotion.dto.response.PublicPromotionOfferResponse;
import com.project.promotionservice.promotion.entity.CampaignPresentation;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignPresentationStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionPlacement;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.mapper.PromotionMapper;
import com.project.promotionservice.promotion.repository.CampaignPresentationRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class CampaignPresentationService {

    private final CampaignPresentationRepository presentationRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionMapper promotionMapper;
    private final PromotionAssetStorage assetStorage;

    public CampaignPresentationService(
            CampaignPresentationRepository presentationRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionRepository promotionRepository,
            PromotionMapper promotionMapper,
            PromotionAssetStorage assetStorage) {
        this.presentationRepository = presentationRepository;
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
        this.promotionMapper = promotionMapper;
        this.assetStorage = assetStorage;
    }

    @Transactional(readOnly = true)
    public CampaignPresentationResponse get(String campaignPublicId) {
        PromotionCampaign campaign = requireCampaign(campaignPublicId);
        return presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull(campaignPublicId)
                .map(this::response)
                .orElseGet(() -> draftResponse(campaign));
    }

    @Transactional
    public CampaignPresentationResponse update(
            String campaignPublicId,
            CampaignPresentationUpdateRequest request,
            String actor) {
        PromotionCampaign campaign = requireCampaign(campaignPublicId);
        CampaignPresentation presentation = findOrCreate(campaign);
        validatePrimaryPromotion(campaignPublicId, request.primaryPromotionPublicId());

        presentation.setHeadline(request.headline().trim());
        presentation.setSummary(request.summary().trim());
        presentation.setImageAltText(blankToNull(request.imageAltText()));
        presentation.setFeatured(Boolean.TRUE.equals(request.featured()));
        presentation.setDisplayOrder(request.displayOrder() == null ? 100 : request.displayOrder());
        presentation.setShowOnHome(Boolean.TRUE.equals(request.showOnHome()));
        presentation.setShowInPromotionCenter(Boolean.TRUE.equals(request.showInPromotionCenter()));
        presentation.setShowInWallet(Boolean.TRUE.equals(request.showInWallet()));
        presentation.setPrimaryPromotionPublicId(blankToNull(request.primaryPromotionPublicId()));
        presentation.setUpdatedBy(actor);
        if (presentation.getStatus() == CampaignPresentationStatus.PUBLISHED) {
            presentation.setStatus(CampaignPresentationStatus.DRAFT);
            presentation.setPublishedAt(null);
            presentation.setPublishedBy(null);
        }
        return response(presentationRepository.save(presentation));
    }

    @Transactional
    public CampaignPresentationResponse uploadCover(
            String campaignPublicId,
            MultipartFile file,
            String actor) {
        PromotionCampaign campaign = requireCampaign(campaignPublicId);
        CampaignPresentation presentation = findOrCreate(campaign);
        PromotionAssetStorage.StoredAsset stored = assetStorage.storeCover(file);
        assetStorage.deleteOnRollback(stored.storageKey());

        String oldStorageKey = presentation.getCoverImageStorageKey();
        String oldProvider = presentation.getCoverImageStorageProvider();
        presentation.setCoverImageUrl(stored.url());
        presentation.setCoverImageStorageKey(stored.storageKey());
        presentation.setCoverImageStorageProvider(assetStorage.provider());
        presentation.setCoverImageContentType(stored.contentType());
        presentation.setCoverImageBytes(stored.bytes());
        presentation.setUpdatedBy(actor);
        CampaignPresentation saved = presentationRepository.save(presentation);

        if (oldStorageKey != null && assetStorage.provider().equalsIgnoreCase(oldProvider)) {
            assetStorage.deleteAfterCommit(oldStorageKey);
        }
        return response(saved);
    }

    @Transactional
    public CampaignPresentationResponse removeCover(
            String campaignPublicId,
            String actor) {
        requireCampaign(campaignPublicId);
        CampaignPresentation presentation = requirePresentation(campaignPublicId);
        String oldStorageKey = presentation.getCoverImageStorageKey();
        String oldProvider = presentation.getCoverImageStorageProvider();
        presentation.setCoverImageUrl(null);
        presentation.setCoverImageStorageKey(null);
        presentation.setCoverImageStorageProvider(null);
        presentation.setCoverImageContentType(null);
        presentation.setCoverImageBytes(null);
        presentation.setUpdatedBy(actor);
        if (presentation.getStatus() == CampaignPresentationStatus.PUBLISHED) {
            presentation.setStatus(CampaignPresentationStatus.DRAFT);
            presentation.setPublishedAt(null);
            presentation.setPublishedBy(null);
        }
        CampaignPresentation saved = presentationRepository.save(presentation);
        if (oldStorageKey != null && assetStorage.provider().equalsIgnoreCase(oldProvider)) {
            assetStorage.deleteAfterCommit(oldStorageKey);
        }
        return response(saved);
    }

    @Transactional
    public CampaignPresentationResponse publish(String campaignPublicId, String actor) {
        PromotionCampaign campaign = requireCampaign(campaignPublicId);
        CampaignPresentation presentation = requirePresentation(campaignPublicId);
        validatePublishable(campaign, presentation);
        presentation.setStatus(CampaignPresentationStatus.PUBLISHED);
        presentation.setPublishedAt(Instant.now());
        presentation.setPublishedBy(actor);
        presentation.setUpdatedBy(actor);
        return response(presentationRepository.save(presentation));
    }

    @Transactional
    public CampaignPresentationResponse unpublish(String campaignPublicId, String actor) {
        requireCampaign(campaignPublicId);
        CampaignPresentation presentation = requirePresentation(campaignPublicId);
        presentation.setStatus(CampaignPresentationStatus.DRAFT);
        presentation.setPublishedAt(null);
        presentation.setPublishedBy(null);
        presentation.setUpdatedBy(actor);
        return response(presentationRepository.save(presentation));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PublicPromotionOfferResponse> publicOffers(
            PromotionPlacement placement,
            Pageable pageable) {
        Instant now = Instant.now();
        Page<CampaignPresentation> page = switch (placement) {
            case HOME -> presentationRepository.findPublicHome(
                    CampaignPresentationStatus.PUBLISHED,
                    CampaignStatus.ACTIVE,
                    LegalStatus.PASSED,
                    now,
                    pageable);
            case PROMOTION_CENTER ->
                    presentationRepository.findPublicPromotionCenter(
                            CampaignPresentationStatus.PUBLISHED,
                            CampaignStatus.ACTIVE,
                            LegalStatus.PASSED,
                            now,
                            pageable);
            case WALLET -> presentationRepository.findPublicWallet(
                    CampaignPresentationStatus.PUBLISHED,
                    CampaignStatus.ACTIVE,
                    LegalStatus.PASSED,
                    now,
                    pageable);
        };
        List<PublicPromotionOfferResponse> content = page.getContent().stream()
                .map(item -> publicOffer(item, now))
                .toList();
        return new PagedResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public PromotionAssetStorage.LoadedAsset loadAsset(String storageKey) {
        return assetStorage.load(storageKey);
    }

    private PublicPromotionOfferResponse publicOffer(
            CampaignPresentation presentation,
            Instant now) {
        PromotionCampaign campaign = requireCampaign(presentation.getCampaignPublicId());
        Promotion promotion = resolvePrimaryPromotion(presentation, now);
        return new PublicPromotionOfferResponse(
                campaign.getPublicId(),
                campaign.getSlug(),
                presentation.getHeadline(),
                presentation.getSummary(),
                presentation.getCoverImageUrl(),
                presentation.getImageAltText(),
                Boolean.TRUE.equals(presentation.getFeatured()),
                presentation.getDisplayOrder(),
                campaign.getStartAt(),
                campaign.getEndAt(),
                promotion == null ? null : promotionMapper.response(promotion, campaign));
    }

    private Promotion resolvePrimaryPromotion(
            CampaignPresentation presentation,
            Instant now) {
        List<Promotion> active = promotionRepository
                .findByCampaignPublicIdAndDeletedAtIsNullOrderByPriorityAsc(
                        presentation.getCampaignPublicId())
                .stream()
                .filter(item -> item.getStatus() == PromotionStatus.ACTIVE)
                .filter(item -> !item.getValidFrom().isAfter(now) && item.getValidTo().isAfter(now))
                .toList();
        if (presentation.getPrimaryPromotionPublicId() != null) {
            Promotion selected = active.stream()
                    .filter(item -> presentation.getPrimaryPromotionPublicId()
                            .equals(item.getPublicId()))
                    .findFirst()
                    .orElse(null);
            if (selected != null) return selected;
        }
        return active.stream()
                .min(Comparator
                        .comparing((Promotion item) ->
                                !(item.getPromotionType() == PromotionType.VOUCHER
                                        && Boolean.TRUE.equals(item.getPublicVisible())))
                        .thenComparing(Promotion::getPriority))
                .orElse(null);
    }

    private CampaignPresentation findOrCreate(PromotionCampaign campaign) {
        return presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull(campaign.getPublicId())
                .orElseGet(() -> {
                    CampaignPresentation item = new CampaignPresentation();
                    item.setCampaignPublicId(campaign.getPublicId());
                    item.setHeadline(campaign.getName());
                    item.setSummary(defaultSummary(campaign));
                    return item;
                });
    }

    private void validatePublishable(
            PromotionCampaign campaign,
            CampaignPresentation presentation) {
        if (campaign.getStatus() != CampaignStatus.SCHEDULED
                && campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw conflict("Campaign must be scheduled or active before its presentation is published");
        }
        if (campaign.getApprovalStatus() != CampaignApprovalStatus.APPROVED
                || campaign.getLegalStatus() != LegalStatus.PASSED) {
            throw conflict("Campaign approval and legal review must pass before publication");
        }
        if (Boolean.TRUE.equals(campaign.getTestData())
                || Boolean.TRUE.equals(campaign.getKillSwitch())) {
            throw conflict("Test or emergency-stopped campaigns cannot be published to customers");
        }
        if (!Boolean.TRUE.equals(presentation.getShowOnHome())
                && !Boolean.TRUE.equals(presentation.getShowInPromotionCenter())
                && !Boolean.TRUE.equals(presentation.getShowInWallet())) {
            throw badRequest("Choose at least one customer placement before publication");
        }
        if (presentation.getCoverImageUrl() == null
                || presentation.getCoverImageUrl().isBlank()) {
            throw badRequest("A cover image is required before publication");
        }
        if (presentation.getImageAltText() == null
                || presentation.getImageAltText().isBlank()) {
            throw badRequest("Image alternative text is required before publication");
        }
    }

    private void validatePrimaryPromotion(String campaignPublicId, String promotionPublicId) {
        String normalized = blankToNull(promotionPublicId);
        if (normalized == null) return;
        Promotion promotion = promotionRepository
                .findByPublicIdAndDeletedAtIsNull(normalized)
                .orElseThrow(() -> badRequest("Primary promotion was not found"));
        if (!campaignPublicId.equals(promotion.getCampaignPublicId())) {
            throw badRequest("Primary promotion must belong to the campaign");
        }
    }

    private CampaignPresentation requirePresentation(String campaignPublicId) {
        return presentationRepository
                .findByCampaignPublicIdAndDeletedAtIsNull(campaignPublicId)
                .orElseThrow(() -> new BusinessException(
                        "CAMPAIGN_PRESENTATION_NOT_FOUND",
                        "Campaign presentation was not found",
                        HttpStatus.NOT_FOUND));
    }

    private PromotionCampaign requireCampaign(String campaignPublicId) {
        return campaignRepository
                .findByPublicIdAndDeletedAtIsNull(campaignPublicId)
                .orElseThrow(() -> new BusinessException(
                        "CAMPAIGN_NOT_FOUND",
                        "Promotion campaign was not found",
                        HttpStatus.NOT_FOUND));
    }

    private CampaignPresentationResponse response(CampaignPresentation item) {
        return new CampaignPresentationResponse(
                item.getPublicId(),
                item.getVersion(),
                item.getCampaignPublicId(),
                item.getStatus(),
                item.getHeadline(),
                item.getSummary(),
                item.getCoverImageUrl(),
                item.getCoverImageStorageProvider(),
                item.getCoverImageContentType(),
                item.getCoverImageBytes(),
                item.getImageAltText(),
                Boolean.TRUE.equals(item.getFeatured()),
                item.getDisplayOrder(),
                Boolean.TRUE.equals(item.getShowOnHome()),
                Boolean.TRUE.equals(item.getShowInPromotionCenter()),
                Boolean.TRUE.equals(item.getShowInWallet()),
                item.getPrimaryPromotionPublicId(),
                item.getPublishedAt(),
                item.getPublishedBy(),
                item.getUpdatedAt(),
                item.getUpdatedBy());
    }

    private CampaignPresentationResponse draftResponse(PromotionCampaign campaign) {
        return new CampaignPresentationResponse(
                null,
                0,
                campaign.getPublicId(),
                CampaignPresentationStatus.DRAFT,
                campaign.getName(),
                defaultSummary(campaign),
                null,
                null,
                null,
                null,
                null,
                false,
                100,
                false,
                false,
                false,
                null,
                null,
                null,
                campaign.getUpdatedAt(),
                campaign.getUpdatedBy());
    }

    private String defaultSummary(PromotionCampaign campaign) {
        String description = blankToNull(campaign.getDescription());
        return description == null
                ? "Khám phá quyền lợi trong chương trình " + campaign.getName()
                : description.length() <= 500 ? description : description.substring(0, 500);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(
                "CAMPAIGN_PRESENTATION_INVALID", message, HttpStatus.BAD_REQUEST);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(
                "CAMPAIGN_PRESENTATION_NOT_PUBLISHABLE", message, HttpStatus.CONFLICT);
    }
}
