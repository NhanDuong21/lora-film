package com.project.promotionservice.promotion.mapper;

import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignDetailResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.PromotionRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CampaignMapper {

    private final RuleMapper ruleMapper;

    public CampaignMapper(RuleMapper ruleMapper) {
        this.ruleMapper = ruleMapper;
    }

    public PromotionCampaign toEntity(CampaignCreateRequest request) {
        if (request == null) {
            return null;
        }

        PromotionCampaign entity = new PromotionCampaign();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setCampaignType(request.getCampaignType());
        entity.setFundingSource(request.getFundingSource());
        entity.setPartnerPublicId(request.getPartnerPublicId());
        entity.setPriority(request.getPriority());
        entity.setStackable(request.getStackable());
        entity.setExclusiveCampaign(request.getExclusiveCampaign());
        entity.setAutoActivate(request.getAutoActivate());
        entity.setAutoComplete(request.getAutoComplete());
        entity.setAutoPauseWhenBudgetExceeded(request.getAutoPauseWhenBudgetExceeded());
        entity.setTimezone(request.getTimezone());
        entity.setStartAt(request.getStartAt());
        entity.setEndAt(request.getEndAt());
        entity.setBudgetAmount(request.getBudgetAmount());
        entity.setBudgetRemaining(request.getBudgetAmount()); // remaining initially = amount
        entity.setMaxRedemptions(request.getMaxRedemptions());
        entity.setMaxRedemptionsPerUser(request.getMaxRedemptionsPerUser());
        entity.setLegalNotificationRef(request.getLegalNotificationRef());
        entity.setRemarks(request.getRemarks());

        return entity;
    }

    public CampaignResponse toResponse(PromotionCampaign entity) {
        if (entity == null) {
            return null;
        }

        CampaignResponse response = new CampaignResponse();
        mapCommon(entity, response);
        return response;
    }

    public CampaignDetailResponse toDetailResponse(PromotionCampaign entity, List<PromotionRule> rules) {
        if (entity == null) {
            return null;
        }

        CampaignDetailResponse response = new CampaignDetailResponse();
        mapCommon(entity, response);

        if (rules != null) {
            response.setRules(rules.stream()
                    .map(ruleMapper::toResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setRules(new ArrayList<>());
        }

        return response;
    }

    private void mapCommon(PromotionCampaign entity, CampaignResponse response) {
        response.setPublicId(entity.getPublicId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setSlug(entity.getSlug());
        response.setDescription(entity.getDescription());
        response.setCampaignType(entity.getCampaignType());
        response.setFundingSource(entity.getFundingSource());
        response.setPartnerPublicId(entity.getPartnerPublicId());
        response.setStatus(entity.getStatus());
        response.setApprovalStatus(entity.getApprovalStatus());
        response.setLegalStatus(entity.getLegalStatus());
        response.setPriority(entity.getPriority());
        response.setStackable(entity.getStackable());
        response.setExclusiveCampaign(entity.getExclusiveCampaign());
        response.setAutoActivate(entity.getAutoActivate());
        response.setAutoComplete(entity.getAutoComplete());
        response.setAutoPauseWhenBudgetExceeded(entity.getAutoPauseWhenBudgetExceeded());
        response.setKillSwitch(entity.getKillSwitch());
        response.setTimezone(entity.getTimezone());
        response.setStartAt(entity.getStartAt());
        response.setEndAt(entity.getEndAt());
        response.setPublishedAt(entity.getPublishedAt());
        response.setApprovedAt(entity.getApprovedAt());
        response.setApprovedBy(entity.getApprovedBy());
        response.setBudgetAmount(entity.getBudgetAmount());
        response.setBudgetUsed(entity.getBudgetUsed());
        response.setBudgetReserved(entity.getBudgetReserved());
        response.setBudgetRemaining(entity.getBudgetRemaining());
        response.setMaxRedemptions(entity.getMaxRedemptions());
        response.setRedemptionCount(entity.getRedemptionCount());
        response.setMaxRedemptionsPerUser(entity.getMaxRedemptionsPerUser());
        response.setLegalNotificationRef(entity.getLegalNotificationRef());
        response.setRemarks(entity.getRemarks());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setUpdatedBy(entity.getUpdatedBy());
    }
}
