package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEvent;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.integration.outbox.PromotionOutboxEnvelopeFactory;
import com.project.promotionservice.promotion.dto.response.ApprovalHistoryResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.ApprovalHistory;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.enums.ApprovalAction;
import com.project.promotionservice.promotion.enums.ApprovalTargetType;
import com.project.promotionservice.promotion.enums.CampaignApprovalStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.mapper.ApprovalMapper;
import com.project.promotionservice.promotion.mapper.CampaignMapper;
import com.project.promotionservice.promotion.repository.ApprovalHistoryRepository;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.service.ApprovalService;
import com.project.promotionservice.common.audit.Auditable;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private BigDecimal budgetLimit = new BigDecimal("50000000.00");

    @Value("${promotion.approval.high-budget-threshold:50000000.00}")
    void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    private final PromotionCampaignRepository campaignRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final PromotionOutboxEventRepository outboxEventRepository;
    private final CampaignMapper campaignMapper;
    private final ApprovalMapper approvalMapper;
    private final ObjectMapper objectMapper;
    private final PromotionOutboxEnvelopeFactory envelopeFactory;

    public ApprovalServiceImpl(PromotionCampaignRepository campaignRepository,
            ApprovalHistoryRepository approvalHistoryRepository,
            PromotionOutboxEventRepository outboxEventRepository,
            CampaignMapper campaignMapper,
            ApprovalMapper approvalMapper,
            ObjectMapper objectMapper,
            PromotionOutboxEnvelopeFactory envelopeFactory) {
        this.campaignRepository = campaignRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.campaignMapper = campaignMapper;
        this.approvalMapper = approvalMapper;
        this.objectMapper = objectMapper;
        this.envelopeFactory = envelopeFactory;
    }

    @Override
    @Transactional
    @Auditable(action = "CAMPAIGN_APPROVE", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse approveCampaign(String publicId, String comment, String approver, List<String> capabilities) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted",
                    HttpStatus.BAD_REQUEST);
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is already published or cancelled",
                    HttpStatus.BAD_REQUEST);
        }

        if (campaign.getApprovalStatus() != CampaignApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is not in PENDING status",
                    HttpStatus.BAD_REQUEST);
        }

        // 4-eyes principle check
        if (approver.equalsIgnoreCase(campaign.getCreatedBy())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Creator cannot approve their own campaign", HttpStatus.BAD_REQUEST);
        }

        // Budget authority checks
        String requiredCapability = campaign.getRequiredApprovalCapability();
        if (requiredCapability == null || requiredCapability.isBlank()) {
            requiredCapability = campaign.getBudgetAmount().compareTo(budgetLimit) > 0
                    ? "PROMOTION_APPROVE_HIGH_BUDGET"
                    : "PROMOTION_APPROVE_STANDARD";
        }
        if (!capabilities.contains(requiredCapability)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Approver lacks capability " + requiredCapability,
                    HttpStatus.FORBIDDEN);
        }

        String oldStatus = campaign.getApprovalStatus().name();
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setApprovedBy(approver);
        campaign.setApprovedAt(Instant.now());
        campaign.setUpdatedBy(approver);

        PromotionCampaign saved = campaignRepository.save(campaign);

        // Record approval history
        ApprovalHistory history = new ApprovalHistory();
        history.setTargetType(ApprovalTargetType.CAMPAIGN);
        history.setTargetPublicId(publicId);
        history.setAction(ApprovalAction.APPROVE);
        history.setOldStatus(oldStatus);
        history.setNewStatus(CampaignApprovalStatus.APPROVED.name());
        history.setApproverPublicId(approver);
        history.setComment(comment);
        history.setApprovedAt(Instant.now());
        history.setCreatedBy(approver);
        history.setUpdatedBy(approver);
        approvalHistoryRepository.save(history);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_APPROVED", campaignMapper.toResponse(saved),
                "promotion.campaign.lifecycle", approver);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "CAMPAIGN_REJECT", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse rejectCampaign(String publicId, String comment, String approver, List<String> capabilities) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted",
                    HttpStatus.BAD_REQUEST);
        }

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is already published or cancelled",
                    HttpStatus.BAD_REQUEST);
        }

        if (campaign.getApprovalStatus() != CampaignApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is not in PENDING status",
                    HttpStatus.BAD_REQUEST);
        }

        if (approver.equalsIgnoreCase(campaign.getCreatedBy())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Creator cannot reject their own campaign",
                    HttpStatus.BAD_REQUEST);
        }
        boolean authorized = capabilities.contains("PROMOTION_APPROVE_STANDARD")
                || capabilities.contains("PROMOTION_APPROVE_HIGH_BUDGET");
        if (!authorized) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Approver lacks campaign approval capability", HttpStatus.FORBIDDEN);
        }

        String oldStatus = campaign.getApprovalStatus().name();
        campaign.setApprovalStatus(CampaignApprovalStatus.REJECTED);
        campaign.setUpdatedBy(approver);

        PromotionCampaign saved = campaignRepository.save(campaign);

        // Record approval history
        ApprovalHistory history = new ApprovalHistory();
        history.setTargetType(ApprovalTargetType.CAMPAIGN);
        history.setTargetPublicId(publicId);
        history.setAction(ApprovalAction.REJECT);
        history.setOldStatus(oldStatus);
        history.setNewStatus(CampaignApprovalStatus.REJECTED.name());
        history.setApproverPublicId(approver);
        history.setComment(comment);
        history.setApprovedAt(Instant.now());
        history.setCreatedBy(approver);
        history.setUpdatedBy(approver);
        approvalHistoryRepository.save(history);

        recordOutboxEvent("CAMPAIGN", saved.getPublicId(), "CAMPAIGN_REJECTED", campaignMapper.toResponse(saved),
                "promotion.campaign.lifecycle", approver);

        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Auditable(action = "CAMPAIGN_OVERRIDE_APPROVE", entityType = "PROMOTION_CAMPAIGN")
    public CampaignResponse overrideApproval(
            String publicId, String campaignCode, String incidentReference, String reason,
            String approver, List<String> capabilities) {
        if (!capabilities.contains("PROMOTION_OVERRIDE")) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "Approval override capability is required", HttpStatus.FORBIDDEN);
        }
        PromotionCampaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
        if (!campaign.getCode().equals(campaignCode)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Campaign code confirmation does not match", HttpStatus.BAD_REQUEST);
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Override reason is required", HttpStatus.BAD_REQUEST);
        }
        if (incidentReference == null || incidentReference.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Incident reference is required for an approval override",
                    HttpStatus.BAD_REQUEST);
        }
        if (campaign.getStatus() != CampaignStatus.DRAFT
                || campaign.getApprovalStatus() != CampaignApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Only a pending draft campaign can be override-approved",
                    HttpStatus.CONFLICT);
        }
        String oldStatus = campaign.getApprovalStatus().name();
        campaign.setApprovalStatus(CampaignApprovalStatus.APPROVED);
        campaign.setApprovedBy(approver);
        campaign.setApprovedAt(Instant.now());
        campaign.setUpdatedBy(approver);
        PromotionCampaign saved = campaignRepository.save(campaign);

        ApprovalHistory history = new ApprovalHistory();
        history.setTargetType(ApprovalTargetType.CAMPAIGN);
        history.setTargetPublicId(publicId);
        history.setAction(ApprovalAction.OVERRIDE_APPROVE);
        history.setOldStatus(oldStatus);
        history.setNewStatus(CampaignApprovalStatus.APPROVED.name());
        history.setApproverPublicId(approver);
        history.setComment("EMERGENCY_OVERRIDE [" + incidentReference.trim()
                + "]: " + reason.trim());
        history.setApprovedAt(Instant.now());
        history.setCreatedBy(approver);
        history.setUpdatedBy(approver);
        approvalHistoryRepository.save(history);
        recordOutboxEvent("CAMPAIGN", saved.getPublicId(),
                "CAMPAIGN_APPROVAL_OVERRIDDEN", campaignMapper.toResponse(saved),
                "promotion.campaign.lifecycle", approver);
        return campaignMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalHistoryResponse> getApprovalHistory(String targetPublicId) {
        List<ApprovalHistory> historyList = approvalHistoryRepository
                .findByTargetTypeAndTargetPublicIdAndDeletedAtIsNullOrderByApprovedAtDesc(ApprovalTargetType.CAMPAIGN,
                        targetPublicId);

        return historyList.stream()
                .map(approvalMapper::toResponse)
                .collect(Collectors.toList());
    }

    private void recordOutboxEvent(String aggregateType, String aggregatePublicId, String eventType, Object payload,
            String topic, String actor) {
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
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Failed to record outbox event: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
