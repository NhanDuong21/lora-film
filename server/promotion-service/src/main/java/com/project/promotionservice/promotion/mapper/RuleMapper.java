package com.project.promotionservice.promotion.mapper;

import com.project.promotionservice.promotion.dto.request.RuleCreateRequest;
import com.project.promotionservice.promotion.dto.response.RuleResponse;
import com.project.promotionservice.promotion.entity.PromotionRule;
import org.springframework.stereotype.Component;

@Component
public class RuleMapper {

    public PromotionRule toEntity(RuleCreateRequest request) {
        if (request == null) {
            return null;
        }

        PromotionRule entity = new PromotionRule();
        entity.setCampaignPublicId(request.getCampaignPublicId());
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setRuleType(request.getRuleType());
        entity.setPriority(request.getPriority());
        entity.setExecutionOrder(request.getExecutionOrder());
        entity.setStackable(request.getStackable());
        entity.setStopFurtherRules(request.getStopFurtherRules());
        entity.setEnabled(request.getEnabled());
        entity.setConditionsJson(request.getConditionsJson());
        entity.setActionsJson(request.getActionsJson());
        entity.setMetadataJson(request.getMetadataJson());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());

        return entity;
    }

    public RuleResponse toResponse(PromotionRule entity) {
        if (entity == null) {
            return null;
        }

        RuleResponse response = new RuleResponse();
        response.setPublicId(entity.getPublicId());
        response.setCampaignPublicId(entity.getCampaignPublicId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setRuleType(entity.getRuleType());
        response.setPriority(entity.getPriority());
        response.setExecutionOrder(entity.getExecutionOrder());
        response.setStackable(entity.getStackable());
        response.setStopFurtherRules(entity.getStopFurtherRules());
        response.setEnabled(entity.getEnabled());
        response.setConditionsJson(entity.getConditionsJson());
        response.setActionsJson(entity.getActionsJson());
        response.setMetadataJson(entity.getMetadataJson());
        response.setEffectiveFrom(entity.getEffectiveFrom());
        response.setEffectiveTo(entity.getEffectiveTo());
        response.setVersion(entity.getVersion());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedBy(entity.getCreatedBy());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setUpdatedBy(entity.getUpdatedBy());

        return response;
    }
}
