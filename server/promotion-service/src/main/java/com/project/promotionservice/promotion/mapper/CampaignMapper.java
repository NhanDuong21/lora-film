package com.project.promotionservice.promotion.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignDetailResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.Promotion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CampaignMapper {

    private final PromotionMapper promotionMapper;
    private final ObjectMapper objectMapper;

    public CampaignMapper(PromotionMapper promotionMapper, ObjectMapper objectMapper) {
        this.promotionMapper = promotionMapper;
        this.objectMapper = objectMapper;
    }

    public PromotionCampaign toEntity(CampaignCreateRequest request) {
        if (request == null) {
            return null;
        }

        PromotionCampaign entity = new PromotionCampaign();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
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

    public CampaignDetailResponse toDetailResponse(
            PromotionCampaign entity, List<Promotion> promotions) {
        if (entity == null) {
            return null;
        }

        CampaignDetailResponse response = new CampaignDetailResponse();
        mapCommon(entity, response);

        if (promotions != null) {
            response.setPromotions(promotions.stream()
                    .map(promotion -> promotionMapper.response(promotion, entity))
                    .collect(Collectors.toList()));
        } else {
            response.setPromotions(new ArrayList<>());
        }

        return response;
    }

    private void mapCommon(PromotionCampaign entity, CampaignResponse response) {
        response.setPublicId(entity.getPublicId());
        response.setVersion(entity.getVersion());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setSlug(entity.getSlug());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setApprovalStatus(entity.getApprovalStatus());
        response.setLegalStatus(entity.getLegalStatus());
        response.setScopeType(entity.getScopeType());
        response.setCinemaScope(readCinemaScope(entity.getCinemaScopeJson()));
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
        response.setApprovalThresholdApplied(entity.getApprovalThresholdApplied());
        response.setApprovalPolicyVersion(entity.getApprovalPolicyVersion());
        response.setRequiredApprovalCapability(entity.getRequiredApprovalCapability());
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

    private List<String> readCinemaScope(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            while (node != null && node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (node == null || !node.isArray()) return List.of();
            List<String> values = new ArrayList<>();
            node.forEach(value -> {
                if (value.isTextual() && !value.asText().isBlank()) {
                    values.add(value.asText());
                }
            });
            return List.copyOf(values);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
