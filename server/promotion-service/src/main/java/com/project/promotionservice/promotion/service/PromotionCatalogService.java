package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.promotion.dto.request.PromotionUpsertRequest;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.dto.response.PromotionResponse;
import com.project.promotionservice.promotion.dto.response.WalletPromotionResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.mapper.PromotionMapper;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionSpecifications;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PromotionCatalogService {

    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository walletRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionMapper mapper;
    private final PromotionPolicyValidator policyValidator;
    private final CampaignConfigurationPolicy campaignPolicy;
    private final PromotionCatalogEventService eventService;

    public PromotionCatalogService(
            PromotionRepository promotionRepository,
            UserPromotionRepository walletRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionMapper mapper,
            PromotionPolicyValidator policyValidator,
            CampaignConfigurationPolicy campaignPolicy,
            PromotionCatalogEventService eventService) {
        this.promotionRepository = promotionRepository;
        this.walletRepository = walletRepository;
        this.campaignRepository = campaignRepository;
        this.mapper = mapper;
        this.policyValidator = policyValidator;
        this.campaignPolicy = campaignPolicy;
        this.eventService = eventService;
    }

    @Transactional
    public PromotionResponse create(PromotionUpsertRequest request, String actor) {
        PromotionCampaign campaign = requireCampaign(request.campaignPublicId());
        campaignPolicy.requireEditable(campaign);
        validate(request, campaign, null);
        Promotion promotion = mapper.create(request);
        promotion.setCreatedBy(actor);
        promotion.setUpdatedBy(actor);
        Promotion saved = promotionRepository.save(promotion);
        PromotionResponse response = mapper.response(saved);
        eventService.record("PROMOTION", saved.getPublicId(),
                "PROMOTION_CREATED", response, actor);
        return response;
    }

    @Transactional
    public PromotionResponse update(
            String publicId, PromotionUpsertRequest request, String actor) {
        Promotion promotion = requirePromotionForUpdate(publicId);
        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            throw conflict("Active promotion must be paused before editing");
        }
        PromotionCampaign campaign = requireCampaign(request.campaignPublicId());
        campaignPolicy.requireEditable(campaign);
        validate(request, campaign, promotion);
        mapper.apply(promotion, request);
        promotion.setUpdatedBy(actor);
        Promotion saved = promotionRepository.save(promotion);
        PromotionResponse response = mapper.response(saved);
        eventService.record("PROMOTION", saved.getPublicId(),
                "PROMOTION_UPDATED", response, actor);
        return response;
    }

    @Transactional(readOnly = true)
    public PromotionResponse detail(String publicId) {
        return mapper.response(requirePromotion(publicId));
    }

    @Transactional(readOnly = true)
    public PagedResponse<PromotionResponse> search(
            String campaignPublicId,
            PromotionType type,
            PromotionStatus status,
            Boolean publicVisible,
            String keyword,
            Pageable pageable) {
        Page<Promotion> result = promotionRepository.findAll(
                PromotionSpecifications.filter(
                        campaignPublicId, type, status, publicVisible, keyword),
                pageable);
        return page(result.map(mapper::response));
    }

    @Transactional
    public PromotionResponse activate(String publicId, String actor) {
        Promotion promotion = requirePromotionForUpdate(publicId);
        PromotionCampaign campaign = requireCampaign(promotion.getCampaignPublicId());
        requireCampaignCanServePromotion(campaign);
        Instant now = Instant.now();
        if (!now.isBefore(promotion.getValidTo())) {
            throw conflict("Expired promotion cannot be activated");
        }
        promotion.setStatus(PromotionStatus.ACTIVE);
        promotion.setUpdatedBy(actor);
        Promotion saved = promotionRepository.save(promotion);
        PromotionResponse response = mapper.response(saved);
        eventService.record("PROMOTION", saved.getPublicId(),
                "PROMOTION_ACTIVATED", response, actor);
        return response;
    }

    @Transactional
    public PromotionResponse pause(String publicId, String actor) {
        Promotion promotion = requirePromotionForUpdate(publicId);
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            throw conflict("Only an active promotion can be paused");
        }
        promotion.setStatus(PromotionStatus.PAUSED);
        promotion.setUpdatedBy(actor);
        Promotion saved = promotionRepository.save(promotion);
        PromotionResponse response = mapper.response(saved);
        eventService.record("PROMOTION", saved.getPublicId(),
                "PROMOTION_PAUSED", response, actor);
        return response;
    }

    @Transactional
    public PromotionResponse clonePromotion(String publicId, String actor) {
        Promotion source = requirePromotion(publicId);
        Promotion clone = new Promotion();
        clone.setCampaignPublicId(source.getCampaignPublicId());
        clone.setPromotionType(source.getPromotionType());
        clone.setCode(cloneCode(source));
        clone.setName(source.getName() + " (Copy)");
        clone.setDescription(source.getDescription());
        clone.setStatus(PromotionStatus.DRAFT);
        clone.setPublicVisible(false);
        clone.setPriority(source.getPriority());
        clone.setStackable(source.getStackable());
        clone.setConditionsJson(source.getConditionsJson());
        clone.setActionsJson(source.getActionsJson());
        clone.setMetadataJson(source.getMetadataJson());
        clone.setMaxRedemptions(source.getMaxRedemptions());
        clone.setMaxRedemptionsPerUser(source.getMaxRedemptionsPerUser());
        clone.setValidFrom(source.getValidFrom());
        clone.setValidTo(source.getValidTo());
        clone.setCreatedBy(actor);
        clone.setUpdatedBy(actor);
        Promotion saved = promotionRepository.save(clone);
        PromotionResponse response = mapper.response(saved);
        eventService.record("PROMOTION", saved.getPublicId(),
                "PROMOTION_CLONED", response, actor);
        return response;
    }

    @Transactional
    public void delete(String publicId, String actor) {
        Promotion promotion = requirePromotionForUpdate(publicId);
        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            throw conflict("Active promotion must be paused before deletion");
        }
        promotion.setDeletedAt(Instant.now());
        promotion.setDeletedBy(actor);
        promotion.setUpdatedBy(actor);
        promotionRepository.save(promotion);
        eventService.record("PROMOTION", promotion.getPublicId(),
                "PROMOTION_DELETED", mapper.response(promotion), actor);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PromotionResponse> publicPromotions(Pageable pageable) {
        Instant now = Instant.now();
        Page<Promotion> result = promotionRepository.findPublicPromotions(
                PromotionType.VOUCHER, PromotionStatus.ACTIVE,
                CampaignStatus.ACTIVE, now, pageable);
        List<PromotionResponse> visible = result.getContent().stream()
                .map(mapper::response)
                .toList();
        return new PagedResponse<>(visible, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Transactional
    public WalletPromotionResponse claim(
            String promotionPublicId, String userPublicId, String actor) {
        Promotion promotion = requirePromotionForUpdate(promotionPublicId);
        if (promotion.getPromotionType() != PromotionType.VOUCHER
                || !Boolean.TRUE.equals(promotion.getPublicVisible())) {
            throw new BusinessException(
                    "PROMOTION_NOT_CLAIMABLE", "Promotion is not a public voucher",
                    HttpStatus.BAD_REQUEST);
        }
        requireRuntimeActive(promotion, Instant.now());
        return mapper.wallet(issueOne(promotion, userPublicId, actor), promotion);
    }

    @Transactional
    public WalletPromotionResponse redeemCoupon(
            String code, String userPublicId, String actor) {
        Promotion found = promotionRepository
                .findByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
                        PromotionType.COUPON, code.trim())
                .orElseThrow(() -> notFound("Coupon code was not found"));
        Promotion promotion = requirePromotionForUpdate(found.getPublicId());
        requireRuntimeActive(promotion, Instant.now());
        return mapper.wallet(issueOne(promotion, userPublicId, actor), promotion);
    }

    @Transactional
    public PromotionIssueResponse issue(
            String promotionPublicId, List<String> users, String actor) {
        Promotion promotion = requirePromotionForUpdate(promotionPublicId);
        if (promotion.getPromotionType() == PromotionType.AUTO) {
            throw new BusinessException(
                    "PROMOTION_NOT_ISSUABLE", "AUTO promotion does not use the customer wallet",
                    HttpStatus.BAD_REQUEST);
        }
        LinkedHashSet<String> uniqueUsers = new LinkedHashSet<>();
        for (String user : users) {
            if (user != null && !user.isBlank()) {
                uniqueUsers.add(user.trim());
            }
        }
        if (uniqueUsers.isEmpty()) {
            throw new BusinessException(
                    "PROMOTION_ISSUE_EMPTY", "At least one customer is required",
                    HttpStatus.BAD_REQUEST);
        }
        List<WalletPromotionResponse> issued = new ArrayList<>();
        int alreadyOwned = 0;
        for (String user : uniqueUsers) {
            if (walletRepository
                    .existsByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
                            user, promotionPublicId)) {
                alreadyOwned++;
                continue;
            }
            issued.add(mapper.wallet(issueOne(promotion, user, actor), promotion));
        }
        PromotionIssueResponse response = new PromotionIssueResponse(
                issued.size(), alreadyOwned, issued);
        eventService.record("PROMOTION", promotionPublicId,
                "PROMOTION_ISSUED", response, actor);
        return response;
    }

    @Transactional
    public PagedResponse<WalletPromotionResponse> wallet(
            String userPublicId, UserPromotionStatus status, Pageable pageable) {
        Instant now = Instant.now();
        Page<UserPromotion> result = walletRepository.findWallet(
                userPublicId, status, pageable);
        List<UserPromotion> changed = new ArrayList<>();
        for (UserPromotion item : result.getContent()) {
            if (item.getStatus() == UserPromotionStatus.AVAILABLE
                    && !now.isBefore(item.getValidTo())) {
                item.setStatus(UserPromotionStatus.EXPIRED);
                changed.add(item);
            }
        }
        if (!changed.isEmpty()) {
            walletRepository.saveAll(changed);
        }
        Map<String, Promotion> promotions = promotionMap(result.getContent());
        List<WalletPromotionResponse> content = result.getContent().stream()
                .map(item -> mapper.wallet(item,
                        requireMappedPromotion(promotions, item.getPromotionPublicId())))
                .toList();
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    private void validate(
            PromotionUpsertRequest request,
            PromotionCampaign campaign,
            Promotion existing) {
        if (!request.validTo().isAfter(request.validFrom())) {
            throw badRequest("validTo must be after validFrom");
        }
        if (request.validFrom().isBefore(campaign.getStartAt())
                || request.validTo().isAfter(campaign.getEndAt())) {
            throw badRequest("Promotion validity must be inside the campaign period");
        }
        if (existing != null && existing.getPromotionType() != request.promotionType()) {
            throw badRequest("promotionType cannot be changed");
        }
        String code = request.code() == null ? null : request.code().trim();
        if (request.promotionType() != PromotionType.AUTO
                && (code == null || code.isBlank())) {
            throw badRequest("Voucher and coupon promotions require a code");
        }
        if (request.promotionType() != PromotionType.VOUCHER
                && Boolean.TRUE.equals(request.publicVisible())) {
            throw badRequest("Only voucher promotions can be public");
        }
        if (code != null && !code.isBlank()
                && promotionRepository
                .existsByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
                        request.promotionType(), code)
                && (existing == null || !code.equalsIgnoreCase(existing.getCode()))) {
            throw conflict("Promotion code already exists for this type");
        }
        if (request.maxRedemptions() != null
                && request.maxRedemptionsPerUser() > request.maxRedemptions()) {
            throw badRequest("maxRedemptionsPerUser cannot exceed maxRedemptions");
        }
        policyValidator.validatePromotion(request.conditionsJson(), request.actionsJson());
    }

    private UserPromotion issueOne(
            Promotion promotion, String userPublicId, String actor) {
        return walletRepository
                .findByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
                        userPublicId, promotion.getPublicId())
                .orElseGet(() -> {
                    UserPromotion wallet = new UserPromotion();
                    wallet.setUserPublicId(userPublicId);
                    wallet.setPromotionPublicId(promotion.getPublicId());
                    wallet.setStatus(UserPromotionStatus.AVAILABLE);
                    wallet.setClaimedAt(Instant.now());
                    wallet.setValidFrom(promotion.getValidFrom());
                    wallet.setValidTo(promotion.getValidTo());
                    wallet.setMaxUsage(promotion.getMaxRedemptionsPerUser());
                    wallet.setCreatedBy(actor);
                    wallet.setUpdatedBy(actor);
                    UserPromotion saved = walletRepository.save(wallet);
                    eventService.record("USER_PROMOTION", saved.getPublicId(),
                            "PROMOTION_ADDED_TO_WALLET",
                            mapper.wallet(saved, promotion), actor);
                    return saved;
                });
    }

    private PromotionCampaign requireCampaign(String publicId) {
        return campaignRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Promotion campaign was not found"));
    }

    private Promotion requirePromotion(String publicId) {
        return promotionRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> notFound("Promotion was not found"));
    }

    private Promotion requirePromotionForUpdate(String publicId) {
        return promotionRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> notFound("Promotion was not found"));
    }

    private void requireCampaignCanServePromotion(PromotionCampaign campaign) {
        if (campaign.getStatus() == CampaignStatus.CANCELLED
                || campaign.getStatus() == CampaignStatus.COMPLETED
                || Boolean.TRUE.equals(campaign.getKillSwitch())) {
            throw conflict("Campaign cannot serve an active promotion");
        }
    }

    private void requireRuntimeActive(Promotion promotion, Instant now) {
        PromotionCampaign campaign = requireCampaign(promotion.getCampaignPublicId());
        if (promotion.getStatus() != PromotionStatus.ACTIVE
                || now.isBefore(promotion.getValidFrom())
                || !now.isBefore(promotion.getValidTo())
                || !isRuntimeCampaignActive(campaign, now)) {
            throw conflict("Promotion is not active");
        }
    }

    private boolean isRuntimeCampaignActive(PromotionCampaign campaign, Instant now) {
        return campaign != null
                && campaign.getStatus() == CampaignStatus.ACTIVE
                && campaign.getLegalStatus() == LegalStatus.PASSED
                && !Boolean.TRUE.equals(campaign.getKillSwitch())
                && !now.isBefore(campaign.getStartAt())
                && now.isBefore(campaign.getEndAt());
    }

    private Map<String, Promotion> promotionMap(List<UserPromotion> walletItems) {
        List<String> ids = walletItems.stream()
                .map(UserPromotion::getPromotionPublicId)
                .distinct()
                .toList();
        Map<String, Promotion> result = new HashMap<>();
        promotionRepository.findByPublicIdInAndDeletedAtIsNull(ids)
                .forEach(promotion -> result.put(promotion.getPublicId(), promotion));
        return result;
    }

    private Promotion requireMappedPromotion(
            Map<String, Promotion> promotions, String publicId) {
        Promotion promotion = promotions.get(publicId);
        if (promotion == null) {
            throw conflict("Wallet contains a missing promotion template");
        }
        return promotion;
    }

    private String cloneCode(Promotion source) {
        if (source.getPromotionType() == PromotionType.AUTO
                && (source.getCode() == null || source.getCode().isBlank())) {
            return null;
        }
        String prefix = source.getCode() == null
                ? source.getPromotionType().name() : source.getCode();
        return (prefix + "_COPY_" + UUID.randomUUID().toString().substring(0, 6))
                .toUpperCase(Locale.ROOT);
    }

    private <T> PagedResponse<T> page(Page<T> result) {
        return new PagedResponse<>(result.getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isLast());
    }

    private BusinessException notFound(String message) {
        return new BusinessException("PROMOTION_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(
                "PROMOTION_INVALID", message, HttpStatus.BAD_REQUEST);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(
                "PROMOTION_CONFLICT", message, HttpStatus.CONFLICT);
    }
}
