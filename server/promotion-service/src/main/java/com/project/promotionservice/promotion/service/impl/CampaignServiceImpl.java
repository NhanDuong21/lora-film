package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEvent;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.integration.outbox.PromotionOutboxEnvelopeFactory;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.request.CampaignUpdateRequest;
import com.project.promotionservice.promotion.dto.request.LegalReviewRequest;
import com.project.promotionservice.promotion.dto.response.CampaignDetailResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.ApprovalHistory;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.enums.ApprovalAction;
import com.project.promotionservice.promotion.enums.ApprovalTargetType;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignScopeType;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.mapper.CampaignMapper;
import com.project.promotionservice.promotion.repository.ApprovalHistoryRepository;
import com.project.promotionservice.promotion.repository.CampaignSpecification;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRepository;
import com.project.promotionservice.promotion.repository.PromotionRedemptionRepository;
import com.project.promotionservice.promotion.repository.UserPromotionRepository;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import com.project.promotionservice.reservation.repository.PromotionReservationRepository;
import com.project.promotionservice.promotion.enums.LegalStatus;
import com.project.promotionservice.common.audit.Auditable;
import com.project.promotionservice.promotion.service.CampaignService;
import com.project.promotionservice.promotion.service.CampaignConfigurationPolicy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.Collection;
import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class CampaignServiceImpl implements CampaignService {

    @Value("${promotion.approval.high-budget-threshold:50000000.00}")
    private BigDecimal highBudgetThreshold = new BigDecimal("50000000.00");

    @Value("${promotion.approval.policy-version:2026-08-v1}")
    private String approvalPolicyVersion = "2026-08-v1";

    private final PromotionCampaignRepository campaignRepository;
    private final PromotionRepository promotionRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final PromotionOutboxEventRepository outboxEventRepository;
    private final CampaignMapper campaignMapper;
    private final ObjectMapper objectMapper;
    private final PromotionReservationRepository reservationRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final UserPromotionRepository walletRepository;
    private final PromotionOutboxEnvelopeFactory envelopeFactory;
    private final CampaignConfigurationPolicy configurationPolicy;

    public CampaignServiceImpl(PromotionCampaignRepository campaignRepository,
                               PromotionRepository promotionRepository,
                               ApprovalHistoryRepository approvalHistoryRepository,
                               PromotionOutboxEventRepository outboxEventRepository,
                               CampaignMapper campaignMapper,
                               ObjectMapper objectMapper,
                               PromotionReservationRepository reservationRepository,
                               PromotionRedemptionRepository redemptionRepository,
                               UserPromotionRepository walletRepository,
                               PromotionOutboxEnvelopeFactory envelopeFactory,
                               CampaignConfigurationPolicy configurationPolicy) {
        this.campaignRepository = campaignRepository;
        this.promotionRepository = promotionRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.campaignMapper = campaignMapper;
        this.objectMapper = objectMapper;
        this.reservationRepository = reservationRepository;
        this.redemptionRepository = redemptionRepository;
        this.walletRepository = walletRepository;
        this.envelopeFactory = envelopeFactory;
        this.configurationPolicy = configurationPolicy;
    }

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request, String creator) {
        return createCampaign(request, creator, CampaignScopeType.GLOBAL, List.of());
    }

    @Override
    @Transactional
    @Auditable(action = "CAMPAIGN_CREATE", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse createCampaign(
            CampaignCreateRequest request, String creator,
            CampaignScopeType scopeType, Collection<String> cinemaScope) {
        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "End date must be after start date", HttpStatus.BAD_REQUEST);
        }

        if (campaignRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign code already exists", HttpStatus.BAD_REQUEST);
        }

        String slug = slugify(request.getName());
        if (campaignRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        PromotionCampaign campaign = campaignMapper.toEntity(request);
        campaign.setScopeType(scopeType == null ? CampaignScopeType.GLOBAL : scopeType);
        try {
            campaign.setCinemaScopeJson(campaign.getScopeType() == CampaignScopeType.GLOBAL
                    ? null : objectMapper.writeValueAsString(cinemaScope == null ? List.of() : cinemaScope));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist campaign cinema scope", exception);
        }
        campaign.setSlug(slug);
        campaign.setCreatedBy(creator);
        campaign.setUpdatedBy(creator);

        PromotionCampaign saved = campaignRepository.save(campaign);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_CREATED", campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", creator);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(String publicId, CampaignUpdateRequest request, String updater) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        configurationPolicy.requireEditable(campaign);
        requireCampaignConfigurationMutable(campaign);

        if (request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "End date must be after start date", HttpStatus.BAD_REQUEST);
        }

        campaign.setName(request.getName());
        campaign.setDescription(request.getDescription());
        campaign.setPriority(request.getPriority());
        campaign.setStackable(request.getStackable());
        campaign.setExclusiveCampaign(request.getExclusiveCampaign());
        campaign.setAutoActivate(request.getAutoActivate());
        campaign.setAutoComplete(request.getAutoComplete());
        campaign.setAutoPauseWhenBudgetExceeded(request.getAutoPauseWhenBudgetExceeded());
        campaign.setTimezone(request.getTimezone());
        campaign.setStartAt(request.getStartAt());
        campaign.setEndAt(request.getEndAt());

        // Update budget and recalculate remaining
        BigDecimal oldBudget = campaign.getBudgetAmount();
        BigDecimal newBudget = request.getBudgetAmount();
        BigDecimal diff = newBudget.subtract(oldBudget);
        campaign.setBudgetAmount(newBudget);
        campaign.setBudgetRemaining(campaign.getBudgetRemaining().add(diff));

        campaign.setMaxRedemptions(request.getMaxRedemptions());
        campaign.setMaxRedemptionsPerUser(request.getMaxRedemptionsPerUser());
        campaign.setLegalNotificationRef(request.getLegalNotificationRef());
        campaign.setRemarks(request.getRemarks());
        configurationPolicy.markConfigurationChanged(campaign, updater);

        PromotionCampaign saved = campaignRepository.save(campaign);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_UPDATED", campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", updater);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCampaign(String publicId, String deleter) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            return;
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Only a draft campaign can be deleted",
                    HttpStatus.CONFLICT);
        }
        if (promotionRepository.existsByCampaignPublicIdAndDeletedAtIsNull(publicId)
                || redemptionRepository.existsByCampaignPublicIdAndDeletedAtIsNull(publicId)
                || walletRepository.existsByCampaignPublicId(publicId)
                || reservationRepository.countByCampaignAndStatus(
                publicId, ReservationStatus.ACTIVE) > 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign with promotion or ledger references cannot be deleted",
                    HttpStatus.CONFLICT);
        }

        campaign.setDeletedAt(Instant.now());
        campaign.setDeletedBy(deleter);
        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);

        recordOutboxEvent("CAMPAIGN", campaign.getPublicId(), "CAMPAIGN_DELETED", campaignMapper.toResponse(campaign), "promotion.campaign.lifecycle", deleter);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignDetailResponse getCampaign(String publicId) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Campaign has been deleted", HttpStatus.NOT_FOUND);
        }

        List<Promotion> promotions = promotionRepository
                .findByCampaignPublicIdAndDeletedAtIsNullOrderByPriorityAsc(publicId);

        return campaignMapper.toDetailResponse(campaign, promotions);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> searchCampaigns(String name, String code, CampaignStatus status,
                                                           Instant from, Instant to, Pageable pageable) {
        return searchCampaigns(name, code, status, from, to, pageable, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<CampaignResponse> searchCampaigns(
            String name, String code, CampaignStatus status,
            Instant from, Instant to, Pageable pageable,
            Collection<String> accessibleCampaignIds) {
        Specification<PromotionCampaign> spec = Specification.where(CampaignSpecification.isNotDeleted())
                .and(CampaignSpecification.hasNameLike(name))
                .and(CampaignSpecification.hasCode(code))
                .and(CampaignSpecification.hasStatus(status))
                .and(CampaignSpecification.startsAfter(from))
                .and(CampaignSpecification.endsBefore(to))
                .and(CampaignSpecification.publicIdIn(accessibleCampaignIds));

        Page<PromotionCampaign> page = campaignRepository.findAll(spec, pageable);

        List<CampaignResponse> dtoList = page.getContent().stream()
                .map(campaignMapper::toResponse)
                .toList();

        return new PagedResponse<>(
                dtoList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    @Override
    @Transactional
    public CampaignResponse submitCampaign(String publicId, String comment, String user) {
        return submitCampaign(publicId, comment, user, false);
    }

    @Override
    @Transactional
    public CampaignResponse submitCampaign(
            String publicId, String comment, String user,
            boolean approveImmediately) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Only DRAFT campaigns can be submitted for approval", HttpStatus.BAD_REQUEST);
        }

        if (campaign.getApprovalStatus() != CampaignApprovalStatus.DRAFT && campaign.getApprovalStatus() != CampaignApprovalStatus.REJECTED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign cannot be submitted for approval in current state", HttpStatus.BAD_REQUEST);
        }

        requireConfiguredBenefit(campaign);

        String oldStatus = campaign.getApprovalStatus().name();
        CampaignApprovalStatus submittedStatus = approveImmediately
                ? CampaignApprovalStatus.APPROVED
                : CampaignApprovalStatus.PENDING;
        Instant submittedAt = Instant.now();
        campaign.setApprovalStatus(submittedStatus);
        campaign.setApprovalThresholdApplied(highBudgetThreshold);
        campaign.setApprovalPolicyVersion(approvalPolicyVersion);
        campaign.setRequiredApprovalCapability(approveImmediately
                ? null
                : campaign.getBudgetAmount().compareTo(highBudgetThreshold) > 0
                        ? "PROMOTION_APPROVE_HIGH_BUDGET"
                        : "PROMOTION_APPROVE_STANDARD");
        if (approveImmediately) {
            campaign.setApprovedBy(user);
            campaign.setApprovedAt(submittedAt);
        }
        campaign.setUpdatedBy(user);
        PromotionCampaign saved = campaignRepository.save(campaign);

        // Keep an explicit audit trail even when ADMIN does not require another approver.
        ApprovalHistory history = new ApprovalHistory();
        history.setTargetType(ApprovalTargetType.CAMPAIGN);
        history.setTargetPublicId(publicId);
        history.setAction(approveImmediately ? ApprovalAction.APPROVE : ApprovalAction.SUBMIT);
        history.setOldStatus(oldStatus);
        history.setNewStatus(submittedStatus.name());
        history.setApproverPublicId(user);
        history.setComment(approveImmediately
                ? "ADMIN_DIRECT_APPROVAL: " + comment
                : comment);
        history.setApprovedAt(submittedAt);
        history.setCreatedBy(user);
        history.setUpdatedBy(user);
        approvalHistoryRepository.save(history);

        recordOutboxEvent(
                "CAMPAIGN", saved.getPublicId(),
                approveImmediately ? "CAMPAIGN_APPROVED" : "CAMPAIGN_SUBMITTED",
                campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", user);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse publishCampaign(String publicId, String user) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        if (campaign.getApprovalStatus() != CampaignApprovalStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Only APPROVED campaigns can be published", HttpStatus.BAD_REQUEST);
        }
        requireLegalAndBudgetApproval(campaign);
        requireConfiguredBenefit(campaign);

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is already published or cancelled", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        if (!now.isBefore(campaign.getEndAt())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "An ended campaign cannot be published",
                    HttpStatus.BAD_REQUEST);
        } else if (now.isBefore(campaign.getStartAt())) {
            campaign.setStatus(CampaignStatus.SCHEDULED);
        } else {
            campaign.setStatus(CampaignStatus.ACTIVE);
        }

        campaign.setPublishedAt(now);
        campaign.setUpdatedBy(user);
        PromotionCampaign saved = campaignRepository.saveAndFlush(campaign);
        if (saved.getStatus() == CampaignStatus.ACTIVE) {
            activateDraftPromotions(saved.getPublicId(), now, user);
        }

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_PUBLISHED", campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", user);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse activateCampaign(String publicId, String user) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        if (campaign.getStatus() != CampaignStatus.SCHEDULED && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Only SCHEDULED or PAUSED campaigns can be activated", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        requireLegalAndBudgetApproval(campaign);
        requireConfiguredBenefit(campaign);
        if (now.isBefore(campaign.getStartAt())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign cannot be activated before startAt",
                    HttpStatus.BAD_REQUEST);
        }
        if (!now.isBefore(campaign.getEndAt())) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setUpdatedBy(user);
            PromotionCampaign completed = campaignRepository.saveAndFlush(campaign);
            recordOutboxEvent("CAMPAIGN", completed.getPublicId(),
                    "CAMPAIGN_COMPLETED_ON_RESUME",
                    campaignMapper.toResponse(completed),
                    "promotion.campaign.lifecycle", user);
            return campaignMapper.toResponse(completed);
        }
        campaign.setKillSwitch(false);
        campaign.setStatus(CampaignStatus.ACTIVE);

        campaign.setUpdatedBy(user);
        PromotionCampaign saved = campaignRepository.saveAndFlush(campaign);
        activateDraftPromotions(saved.getPublicId(), now, user);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_ACTIVATED", campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", user);

        return campaignMapper.toResponse(saved);
    }

    private void requireLegalAndBudgetApproval(PromotionCampaign campaign) {
        if (campaign.getLegalStatus() != LegalStatus.PASSED) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign must pass legal compliance review before publication or activation",
                    HttpStatus.BAD_REQUEST);
        }
        if (campaign.getBudgetAmount() == null || campaign.getBudgetAmount().signum() <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign budget must be greater than zero",
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional
    @Auditable(action = "LEGAL_REVIEW", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse reviewLegalStatus(
            String publicId, LegalReviewRequest request, String reviewer) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign is deleted",
                    HttpStatus.BAD_REQUEST);
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Legal review must be completed before campaign publication",
                    HttpStatus.BAD_REQUEST);
        }
        if (campaign.getApprovalStatus() != CampaignApprovalStatus.PENDING
                && campaign.getApprovalStatus() != CampaignApprovalStatus.APPROVED) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Legal review is only allowed after the campaign is submitted",
                    HttpStatus.CONFLICT);
        }
        if (request.getStatus() == LegalStatus.PENDING) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Legal review result must be PASSED or FAILED",
                    HttpStatus.BAD_REQUEST);
        }
        campaign.setLegalStatus(request.getStatus());
        if (request.getLegalNotificationRef() != null
                && !request.getLegalNotificationRef().isBlank()) {
            campaign.setLegalNotificationRef(request.getLegalNotificationRef().trim());
        }
        String legalNote = "Legal review " + request.getStatus() + ": "
                + request.getComment().trim();
        campaign.setRemarks(campaign.getRemarks() == null || campaign.getRemarks().isBlank()
                ? legalNote
                : campaign.getRemarks() + " | " + legalNote);
        campaign.setUpdatedBy(reviewer);
        PromotionCampaign saved = campaignRepository.save(campaign);
        recordOutboxEvent(
                "CAMPAIGN",
                saved.getPublicId(),
                "CAMPAIGN_LEGAL_REVIEWED",
                campaignMapper.toResponse(saved),
                "promotion.campaign.lifecycle",
                reviewer);
        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "CAMPAIGN_PAUSE", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse pauseCampaign(String publicId, String user) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        if (campaign.getStatus() != CampaignStatus.ACTIVE && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Only ACTIVE or SCHEDULED campaigns can be paused", HttpStatus.BAD_REQUEST);
        }

        campaign.setStatus(CampaignStatus.PAUSED);
        campaign.setUpdatedBy(user);
        PromotionCampaign saved = campaignRepository.save(campaign);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_PAUSED", campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", user);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "KILL_SWITCH", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse killSwitchCampaign(String publicId, String reason, String user) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Kill switch requires an audit reason",
                    HttpStatus.BAD_REQUEST);
        }
        PromotionCampaign campaign = campaignRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if ((campaign.getStatus() != CampaignStatus.SCHEDULED
                && campaign.getStatus() != CampaignStatus.ACTIVE
                && campaign.getStatus() != CampaignStatus.PAUSED)
                || campaign.getDeletedAt() != null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Kill switch is only available for SCHEDULED, ACTIVE or PAUSED campaigns",
                    HttpStatus.BAD_REQUEST);
        }
        campaign.setKillSwitch(true);
        campaign.setStatus(CampaignStatus.KILLED);
        String auditNote = "Kill switch: " + reason.trim();
        campaign.setRemarks(campaign.getRemarks() == null || campaign.getRemarks().isBlank()
                ? auditNote
                : campaign.getRemarks() + " | " + auditNote);
        campaign.setUpdatedBy(user);
        PromotionCampaign saved = campaignRepository.save(campaign);
        recordOutboxEvent(
                "CAMPAIGN", saved.getPublicId(), "CAMPAIGN_KILL_SWITCHED",
                campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", user);
        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "CAMPAIGN_CANCEL", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse cancelCampaign(String publicId, String user) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }
        if (campaign.getStatus() == CampaignStatus.CANCELLED) {
            return campaignMapper.toResponse(campaign);
        }
        if (campaign.getStatus() == CampaignStatus.COMPLETED
                || campaign.getStatus() == CampaignStatus.KILLED) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "A completed or killed campaign cannot be cancelled",
                    HttpStatus.BAD_REQUEST);
        }

        campaign.setStatus(CampaignStatus.CANCELLED);
        campaign.setUpdatedBy(user);
        PromotionCampaign saved = campaignRepository.save(campaign);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_CANCELLED", campaignMapper.toResponse(saved), "promotion.campaign.lifecycle", user);

        return campaignMapper.toResponse(saved);
    }

    private void requireConfiguredBenefit(PromotionCampaign campaign) {
        boolean configured = promotionRepository.existsConfiguredForCampaign(
                campaign.getPublicId(),
                EnumSet.of(PromotionStatus.DRAFT, PromotionStatus.ACTIVE,
                        PromotionStatus.PAUSED),
                campaign.getStartAt(),
                campaign.getEndAt());
        if (!configured) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign must have at least one configured promotion whose validity overlaps the campaign period",
                    HttpStatus.CONFLICT);
        }
    }

    private void requireCampaignConfigurationMutable(PromotionCampaign campaign) {
        if (redemptionRepository.existsByCampaignPublicIdAndDeletedAtIsNull(
                campaign.getPublicId())
                || walletRepository.existsByCampaignPublicId(campaign.getPublicId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Issued or redeemed campaign configuration is immutable",
                    HttpStatus.CONFLICT);
        }
    }

    private void activateDraftPromotions(
            String campaignPublicId, Instant now, String actor) {
        promotionRepository.activateDraftPromotions(
                campaignPublicId, PromotionStatus.DRAFT,
                PromotionStatus.ACTIVE, now, actor);
    }

    private String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String nfdNormalizedString = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(nfdNormalizedString).replaceAll("");
        return withoutAccents.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private void recordOutboxEvent(String aggregateType, String aggregatePublicId, String eventType, Object payload, String topic, String actor) {
        try {
            PromotionOutboxEvent event = new PromotionOutboxEvent();
            event.setAggregateType(aggregateType);
            event.setAggregatePublicId(aggregatePublicId);
            event.setEventType(eventType);
            event.setEventKey(aggregatePublicId);
            event.setPayload(envelopeFactory.create(event, payload));
            event.setTopicName(topic);
            event.setPublishStatus(OutboxStatus.PENDING);
            event.setCreatedBy(actor);
            event.setUpdatedBy(actor);
            outboxEventRepository.save(event);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to record outbox event: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
