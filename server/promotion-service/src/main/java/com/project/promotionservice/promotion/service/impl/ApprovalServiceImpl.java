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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApprovalServiceImpl implements ApprovalService {

    private static final BigDecimal BUDGET_LIMIT = new BigDecimal("50000000.00");

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
    public CampaignResponse approveCampaign(String publicId, String comment, String approver, List<String> roles) {
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

        boolean isAdmin = roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");

        // 4-eyes principle check
        if (!isAdmin && approver.equalsIgnoreCase(campaign.getCreatedBy())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Creator cannot approve their own campaign", HttpStatus.BAD_REQUEST);
        }

        // Budget authority checks
        if (!isAdmin) {
            boolean isFinanceDirector = roles.contains("ROLE_FINANCE_DIRECTOR") || roles.contains("FINANCE_DIRECTOR");
            boolean isMarketingManager = roles.contains("ROLE_MARKETING_MANAGER")
                    || roles.contains("MARKETING_MANAGER");

            if (campaign.getBudgetAmount().compareTo(BUDGET_LIMIT) > 0) {
                if (!isFinanceDirector) {
                    throw new BusinessException(ErrorCode.FORBIDDEN,
                            "Campaign budget exceeds 50,000,000 VNĐ and requires Finance Director approval",
                            HttpStatus.FORBIDDEN);
                }
            } else {
                if (!isMarketingManager && !isFinanceDirector) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "Approver lacks necessary role authority",
                            HttpStatus.FORBIDDEN);
                }
            }
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
    public CampaignResponse rejectCampaign(String publicId, String comment, String approver, List<String> roles) {
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

        boolean isAdmin = roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");

        // 4-eyes principle check (optional for reject, but let's allow anyone
        // authorized to reject)
        if (!isAdmin && approver.equalsIgnoreCase(campaign.getCreatedBy())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Creator cannot reject their own campaign",
                    HttpStatus.BAD_REQUEST);
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
