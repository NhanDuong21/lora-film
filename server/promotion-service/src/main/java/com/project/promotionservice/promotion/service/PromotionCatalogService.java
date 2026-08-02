package com.project.promotionservice.promotion.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.integration.client.UserRecipientValidationClient;
import com.project.promotionservice.promotion.dto.request.PromotionUpsertRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCloneDraftResponse;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.dto.response.PromotionResponse;
import com.project.promotionservice.promotion.dto.response.WalletPromotionResponse;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import com.project.promotionservice.promotion.mapper.PromotionMapper;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionSpecifications;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Set;

@Service
public class PromotionCatalogService {

    private static final Set<PromotionRedemptionStatus> CAPACITY_STATUSES =
            EnumSet.of(PromotionRedemptionStatus.RESERVED,
                    PromotionRedemptionStatus.CONFIRMED);

    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository walletRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionMapper mapper;
    private final PromotionPolicyValidator policyValidator;
    private final CampaignConfigurationPolicy campaignPolicy;
    private final PromotionCatalogEventService eventService;
    private final UserRecipientValidationClient recipientValidationClient;

    public PromotionCatalogService(
            PromotionRepository promotionRepository,
            UserPromotionRepository walletRepository,
            PromotionCampaignRepository campaignRepository,
            PromotionRedemptionRepository redemptionRepository,
            PromotionMapper mapper,
            PromotionPolicyValidator policyValidator,
            CampaignConfigurationPolicy campaignPolicy,
            PromotionCatalogEventService eventService,
            UserRecipientValidationClient recipientValidationClient) {
        this.promotionRepository = promotionRepository;
        this.walletRepository = walletRepository;
        this.campaignRepository = campaignRepository;
        this.redemptionRepository = redemptionRepository;
        this.mapper = mapper;
        this.policyValidator = policyValidator;
        this.campaignPolicy = campaignPolicy;
        this.eventService = eventService;
        this.recipientValidationClient = recipientValidationClient;
    }

    @Transactional
    public PromotionResponse create(PromotionUpsertRequest request, String actor) {
        request = normalizeCouponCode(request, null);
        PromotionCampaign campaign = requireCampaign(request.campaignPublicId());
        campaignPolicy.requireEditable(campaign);
        validate(request, campaign, null);
        Promotion promotion = mapper.create(request);
        promotion.setClonedFromPublicId(request.clonedFromPublicId());
        promotion.setCreatedBy(actor);
        promotion.setUpdatedBy(actor);
        Promotion saved = promotionRepository.save(promotion);
        PromotionResponse response = mapper.response(saved);
        eventService.record("PROMOTION", saved.getPublicId(),
                "PROMOTION_CREATED", response, actor);
        if (request.clonedFromPublicId() != null
                && !request.clonedFromPublicId().isBlank()) {
            eventService.record("PROMOTION", saved.getPublicId(),
                    "PROMOTION_CLONED_FROM",
                    Map.of("sourcePublicId", request.clonedFromPublicId()), actor);
        }
        return response;
    }

    @Transactional
    public PromotionResponse update(
            String publicId, PromotionUpsertRequest request, String actor) {
        Promotion promotion = requirePromotionForUpdate(publicId);
        requireTemplateMutable(promotion);
        if (promotion.getStatus() == PromotionStatus.ACTIVE) {
            throw conflict("Active promotion must be paused before editing");
        }
        request = normalizeCouponCode(request, promotion);
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

    @Transactional(readOnly = true)
    public PromotionResponse activate(String publicId, String actor) {
        requirePromotion(publicId);
        throw conflict("Promotion lifecycle is controlled by its campaign and scheduler; activate the campaign instead");
    }

    @Transactional(readOnly = true)
    public PromotionResponse pause(String publicId, String actor) {
        requirePromotion(publicId);
        throw conflict("Promotion lifecycle is controlled by its campaign and scheduler; pause the campaign instead");
    }

    @Transactional(readOnly = true)
    public PromotionCloneDraftResponse buildCloneDraft(String publicId) {
        Promotion source = requirePromotion(publicId);
        PromotionCampaign sourceCampaign = requireCampaign(source.getCampaignPublicId());
        boolean editable = campaignPolicy.isEditable(sourceCampaign);
        ValidityWindow window = suggestValidity(
                source, editable ? sourceCampaign : null);
        PromotionResponse sourceData = mapper.response(source);
        String suggestedCode = source.getPromotionType() == PromotionType.COUPON
                ? generatedCouponCode() : null;
        return new PromotionCloneDraftResponse(
                source.getPublicId(), source.getName(),
                editable ? sourceCampaign.getPublicId() : null, editable,
                source.getPromotionType(), suggestedCode, suggestName(source),
                source.getDescription(),
                Boolean.TRUE.equals(source.getPublicVisible()), source.getPriority(),
                source.getStackable(), sourceData.conditionsJson(),
                sourceData.actionsJson(), sourceData.metadataJson(),
                source.getMaxRedemptions(), source.getMaxRedemptionsPerUser(),
                window.from(), window.to(), window.shifted());
    }

    @Deprecated(forRemoval = true)
    @Transactional
    public PromotionResponse clonePromotion(String publicId, String actor) {
        Promotion source = requirePromotion(publicId);
        PromotionCloneDraftResponse draft = buildCloneDraft(publicId);
        if (!draft.sourceCampaignEditable()) {
            throw conflict("Source campaign is locked; choose an editable target campaign");
        }
        String code = draft.suggestedCode();
        if (source.getPromotionType() == PromotionType.VOUCHER) {
            code = cloneCode(source);
        }
        return create(new PromotionUpsertRequest(
                draft.suggestedCampaignPublicId(), draft.promotionType(), code,
                draft.suggestedName(), draft.description(), draft.publicVisible(),
                draft.priority(), draft.stackable(), draft.conditionsJson(),
                draft.actionsJson(), draft.metadataJson(), draft.maxRedemptions(),
                draft.maxRedemptionsPerUser(), draft.suggestedValidFrom(),
                draft.suggestedValidTo(), draft.sourcePublicId()), actor);
    }

    @Transactional
    public void delete(String publicId, String actor) {
        Promotion promotion = requirePromotionForUpdate(publicId);
        if (promotion.getStatus() != PromotionStatus.DRAFT) {
            throw conflict("Only a draft promotion can be deleted");
        }
        requireTemplateMutable(promotion);
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
                CampaignStatus.ACTIVE, LegalStatus.PASSED, now, pageable);
        List<PromotionResponse> visible = result.getContent().stream()
                .map(mapper::response)
                .toList();
        return new PagedResponse<>(visible, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Transactional(readOnly = true)
    public PagedResponse<PromotionResponse> systemPromotions(Pageable pageable) {
        Instant now = Instant.now();
        return page(promotionRepository.findSystemPromotions(
                PromotionType.AUTO, PromotionStatus.ACTIVE, CampaignStatus.ACTIVE,
                LegalStatus.PASSED, now, pageable).map(mapper::response));
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
        UserPromotion existing = walletRepository
                .findFirstByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNullOrderByIdDesc(
                        userPublicId, promotionPublicId)
                .orElse(null);
        if (existing != null) {
            refreshWalletStatus(existing, Instant.now());
            return mapper.wallet(existing, promotion);
        }
        if (promotion.getMaxRedemptions() != null
                && walletRepository.countByPromotionPublicIdAndDeletedAtIsNull(
                promotionPublicId) >= promotion.getMaxRedemptions()) {
            throw conflict("Voucher claim capacity has been reached");
        }
        return mapper.wallet(createGrant(promotion, userPublicId, actor), promotion);
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
        requireRuntimeActive(promotion, Instant.now());
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
        recipientValidationClient.requireAllActive(List.copyOf(uniqueUsers));
        List<WalletPromotionResponse> issued = new ArrayList<>();
        int alreadyOwned = 0;
        int issuedCount = 0;
        for (String user : uniqueUsers) {
            UserPromotion previous = walletRepository
                    .findFirstByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNullOrderByIdDesc(
                            user, promotionPublicId)
                    .orElse(null);
            if (previous != null) {
                refreshWalletStatus(previous, Instant.now());
            }
            if (previous != null && previous.getStatus() == UserPromotionStatus.AVAILABLE) {
                alreadyOwned++;
                continue;
            }
            UserPromotion grant = createGrant(promotion, user, actor);
            issuedCount++;
            if (promotion.getPromotionType() == PromotionType.COUPON) {
                eventService.record("USER_PROMOTION", grant.getPublicId(),
                        "COUPON_ISSUED", Map.of(
                                "userPublicId", user,
                                "couponCode", promotion.getCode(),
                                "promotionName", promotion.getName(),
                                "validTo", promotion.getValidTo().toString(),
                                "deepLink", "/booking"), actor);
            } else {
                issued.add(mapper.wallet(grant, promotion));
            }
        }
        PromotionIssueResponse response = new PromotionIssueResponse(
                issuedCount, alreadyOwned, issued);
        eventService.record("PROMOTION", promotionPublicId,
                "PROMOTION_ISSUED", response, actor);
        return response;
    }

    @Transactional
    public PagedResponse<WalletPromotionResponse> wallet(
            String userPublicId, UserPromotionStatus status, Pageable pageable) {
        Instant now = Instant.now();
        Page<UserPromotion> result = walletRepository.findWallet(
                userPublicId, status, PromotionType.COUPON, pageable);
        List<UserPromotion> changed = new ArrayList<>();
        for (UserPromotion item : result.getContent()) {
            if (refreshWalletStatus(item, now)) {
                changed.add(item);
            }
        }
        if (!changed.isEmpty()) {
            walletRepository.saveAll(changed);
        }
        List<UserPromotion> visibleItems = result.getContent();
        Map<String, Promotion> promotions = promotionMap(visibleItems);
        List<WalletPromotionResponse> content = visibleItems.stream()
                .map(item -> walletResponse(item,
                        requireMappedPromotion(promotions, item.getPromotionPublicId()), now))
                .toList();
        return new PagedResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Transactional
    public WalletPromotionResponse walletDetail(
            String userPublicId, String walletPublicId) {
        Instant now = Instant.now();
        UserPromotion wallet = walletRepository
                .findByPublicIdAndDeletedAtIsNull(walletPublicId)
                .orElseThrow(() -> notFound("Wallet voucher was not found"));
        if (!Objects.equals(wallet.getUserPublicId(), userPublicId)) {
            throw notFound("Wallet voucher was not found");
        }
        boolean changed = refreshWalletStatus(wallet, now);
        if (changed) {
            walletRepository.save(wallet);
        }
        Promotion promotion = requirePromotion(wallet.getPromotionPublicId());
        if (promotion.getPromotionType() == PromotionType.COUPON) {
            throw notFound("Wallet voucher was not found");
        }
        return walletResponse(wallet, promotion, now);
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

    private PromotionUpsertRequest normalizeCouponCode(
            PromotionUpsertRequest request, Promotion existing) {
        if (request.promotionType() != PromotionType.COUPON) {
            return request;
        }
        String requestedCode = request.code() == null ? null : request.code().trim();
        String code = existing != null && existing.getCode() != null
                && !existing.getCode().isBlank()
                ? existing.getCode()
                : request.clonedFromPublicId() != null
                && requestedCode != null && !requestedCode.isBlank()
                ? requestedCode : generatedCouponCode();
        return new PromotionUpsertRequest(
                request.campaignPublicId(), request.promotionType(), code,
                request.name(), request.description(), request.publicVisible(),
                request.priority(), request.stackable(), request.conditionsJson(),
                request.actionsJson(), request.metadataJson(), request.maxRedemptions(),
                request.maxRedemptionsPerUser(), request.validFrom(), request.validTo(),
                request.clonedFromPublicId());
    }

    private UserPromotion createGrant(
            Promotion promotion, String userPublicId, String actor) {
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
        if (promotion.getPromotionType() != PromotionType.COUPON) {
            eventService.record("USER_PROMOTION", saved.getPublicId(),
                    "PROMOTION_ADDED_TO_WALLET",
                    mapper.wallet(saved, promotion), actor);
        }
        return saved;
    }

    private void requireTemplateMutable(Promotion promotion) {
        if (walletRepository.countByPromotionPublicIdAndDeletedAtIsNull(
                promotion.getPublicId()) > 0
                || redemptionRepository.existsByPromotionPublicIdAndDeletedAtIsNull(
                promotion.getPublicId())) {
            throw conflict("Issued or redeemed promotion templates are immutable");
        }
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

    private boolean refreshWalletStatus(UserPromotion item, Instant now) {
        if (item.getStatus() != UserPromotionStatus.AVAILABLE) {
            return false;
        }
        Integer maxUsage = item.getMaxUsage();
        Integer usageCount = item.getUsageCount();
        if (maxUsage != null && usageCount != null && usageCount >= maxUsage) {
            item.setStatus(UserPromotionStatus.USED);
            return true;
        }
        if (!now.isBefore(item.getValidTo())) {
            item.setStatus(UserPromotionStatus.EXPIRED);
            return true;
        }
        return false;
    }

    private boolean isUsableWalletItem(UserPromotion item, Instant now) {
        Integer maxUsage = item.getMaxUsage();
        Integer usageCount = item.getUsageCount();
        return item.getStatus() == UserPromotionStatus.AVAILABLE
                && (usageCount == null || maxUsage == null || usageCount < maxUsage)
                && !now.isBefore(item.getValidFrom())
                && now.isBefore(item.getValidTo());
    }

    private WalletPromotionResponse walletResponse(
            UserPromotion wallet, Promotion promotion, Instant now) {
        String reasonCode = null;
        String reason = null;
        if (!isUsableWalletItem(wallet, now)) {
            reasonCode = "WALLET_NOT_AVAILABLE";
            reason = "Voucher trong vi da het han hoac het luot su dung";
        } else if (promotion.getStatus() != PromotionStatus.ACTIVE
                || now.isBefore(promotion.getValidFrom())
                || !now.isBefore(promotion.getValidTo())) {
            reasonCode = "PROMOTION_NOT_ACTIVE";
            reason = "Voucher chua hoat dong hoac da het hieu luc";
        } else {
            PromotionCampaign campaign = campaignRepository
                    .findByPublicIdAndDeletedAtIsNull(promotion.getCampaignPublicId())
                    .orElse(null);
            if (!isRuntimeCampaignActive(campaign, now)) {
                reasonCode = campaign != null && Boolean.TRUE.equals(campaign.getKillSwitch())
                        ? "CAMPAIGN_KILL_SWITCHED" : "CAMPAIGN_NOT_ACTIVE";
                reason = "Chien dich cua voucher hien khong hoat dong";
            } else if (campaign.getBudgetRemaining()
                    .subtract(campaign.getBudgetReserved()).signum() <= 0) {
                reasonCode = "CAMPAIGN_BUDGET_EXHAUSTED";
                reason = "Ngan sach chien dich da het";
            } else if (promotion.getMaxRedemptions() != null
                    && redemptionRepository
                    .countByPromotionPublicIdAndStatusInAndDeletedAtIsNull(
                            promotion.getPublicId(), CAPACITY_STATUSES)
                    >= promotion.getMaxRedemptions()) {
                reasonCode = "PROMOTION_CAPACITY_EXHAUSTED";
                reason = "Voucher da het luot su dung";
            } else if (redemptionRepository
                    .countByPromotionPublicIdAndUserPublicIdAndStatusInAndDeletedAtIsNull(
                            promotion.getPublicId(), wallet.getUserPublicId(),
                            CAPACITY_STATUSES)
                    >= customerPromotionLimit(promotion, campaign)) {
                reasonCode = "CUSTOMER_PROMOTION_LIMIT_REACHED";
                reason = "Ban da dat gioi han su dung voucher nay";
            } else if (campaign.getMaxRedemptions() != null
                    && redemptionRepository.countCampaignRedemptions(
                            campaign.getPublicId(), CAPACITY_STATUSES)
                    >= campaign.getMaxRedemptions()) {
                reasonCode = "CAMPAIGN_CAPACITY_EXHAUSTED";
                reason = "Chien dich da het luot su dung";
            }
        }
        return new WalletPromotionResponse(
                wallet.getPublicId(), wallet.getUserPublicId(), wallet.getStatus(),
                wallet.getClaimedAt(), wallet.getValidFrom(), wallet.getValidTo(),
                wallet.getUsageCount(), wallet.getMaxUsage(), reasonCode == null,
                reasonCode, reason, mapper.response(promotion));
    }

    private int customerPromotionLimit(
            Promotion promotion, PromotionCampaign campaign) {
        int promotionLimit = promotion.getMaxRedemptionsPerUser() == null
                ? Integer.MAX_VALUE : promotion.getMaxRedemptionsPerUser();
        int campaignCeiling = campaign.getMaxRedemptionsPerUser() == null
                ? Integer.MAX_VALUE : campaign.getMaxRedemptionsPerUser();
        return Math.min(promotionLimit, campaignCeiling);
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

    private String suggestName(Promotion source) {
        String base = source.getName() + " (Copy)";
        if (!promotionRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(base)) {
            return base;
        }
        int sequence = 2;
        String candidate;
        do {
            candidate = source.getName() + " (Copy " + sequence + ")";
            sequence++;
        } while (promotionRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(
                candidate));
        return candidate;
    }

    private ValidityWindow suggestValidity(
            Promotion source, PromotionCampaign targetCampaign) {
        Duration length = Duration.between(source.getValidFrom(), source.getValidTo());
        Instant now = Instant.now();
        boolean expired = !source.getValidTo().isAfter(now);
        Instant from = expired ? now : source.getValidFrom();
        Instant to = from.plus(length);
        if (targetCampaign != null) {
            if (from.isBefore(targetCampaign.getStartAt())) {
                from = targetCampaign.getStartAt();
            }
            if (to.isAfter(targetCampaign.getEndAt())) {
                to = targetCampaign.getEndAt();
            }
        }
        return new ValidityWindow(from, to, expired);
    }

    private String generatedCouponCode() {
        String code;
        do {
            code = "CPN-" + UUID.randomUUID().toString().substring(0, 8)
                    .toUpperCase(Locale.ROOT);
        } while (promotionRepository
                .existsByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
                        PromotionType.COUPON, code));
        return code;
    }

    private record ValidityWindow(Instant from, Instant to, boolean shifted) {
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
