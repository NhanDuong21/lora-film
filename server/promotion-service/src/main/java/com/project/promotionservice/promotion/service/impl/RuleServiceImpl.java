package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEvent;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.promotion.dto.request.RuleCreateRequest;
import com.project.promotionservice.promotion.dto.request.RuleUpdateRequest;
import com.project.promotionservice.promotion.dto.request.RuleCloneRequest;
import com.project.promotionservice.promotion.dto.response.RuleResponse;
import com.project.promotionservice.promotion.entity.PromotionCampaign;
import com.project.promotionservice.promotion.entity.PromotionRule;
import com.project.promotionservice.promotion.mapper.RuleMapper;
import com.project.promotionservice.promotion.repository.PromotionCampaignRepository;
import com.project.promotionservice.promotion.repository.PromotionRuleRepository;
import com.project.promotionservice.promotion.repository.RuleSpecification;
import com.project.promotionservice.promotion.service.RuleService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RuleServiceImpl implements RuleService {

    private final PromotionRuleRepository ruleRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionOutboxEventRepository outboxEventRepository;
    private final RuleMapper ruleMapper;
    private final ObjectMapper objectMapper;

    public RuleServiceImpl(PromotionRuleRepository ruleRepository,
                           PromotionCampaignRepository campaignRepository,
                           PromotionOutboxEventRepository outboxEventRepository,
                           RuleMapper ruleMapper,
                           ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.campaignRepository = campaignRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.ruleMapper = ruleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RuleResponse createRule(RuleCreateRequest request, String creator) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(request.getCampaignPublicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.BAD_REQUEST));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        if (ruleRepository.existsByCodeAndCampaignPublicIdAndDeletedAtIsNull(request.getCode(), request.getCampaignPublicId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Rule code already exists in this campaign", HttpStatus.BAD_REQUEST);
        }

        validateRuleJson(request.getConditionsJson(), request.getActionsJson());

        PromotionRule rule = ruleMapper.toEntity(request);
        rule.setCreatedBy(creator);
        rule.setUpdatedBy(creator);

        PromotionRule saved = ruleRepository.save(rule);

        recordOutboxEvent("RULE", saved.getPublicId(), "RULE_CREATED", ruleMapper.toResponse(saved), "promotion.campaign.lifecycle", creator);

        return ruleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RuleResponse updateRule(String publicId, RuleUpdateRequest request, String updater) {
        PromotionRule rule = ruleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Rule not found", HttpStatus.NOT_FOUND));

        if (rule.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Rule is deleted", HttpStatus.BAD_REQUEST);
        }

        validateRuleJson(request.getConditionsJson(), request.getActionsJson());

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setPriority(request.getPriority());
        rule.setExecutionOrder(request.getExecutionOrder());
        rule.setStackable(request.getStackable());
        rule.setStopFurtherRules(request.getStopFurtherRules());
        rule.setEnabled(request.getEnabled());
        rule.setConditionsJson(request.getConditionsJson());
        rule.setActionsJson(request.getActionsJson());
        rule.setMetadataJson(request.getMetadataJson());
        rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setEffectiveTo(request.getEffectiveTo());
        rule.setUpdatedBy(updater);

        PromotionRule saved = ruleRepository.save(rule);

        recordOutboxEvent("RULE", saved.getPublicId(), "RULE_UPDATED", ruleMapper.toResponse(saved), "promotion.campaign.lifecycle", updater);

        return ruleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRule(String publicId, String deleter) {
        PromotionRule rule = ruleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Rule not found", HttpStatus.NOT_FOUND));

        if (rule.getDeletedAt() != null) {
            return;
        }

        rule.setDeletedAt(Instant.now());
        rule.setDeletedBy(deleter);
        ruleRepository.save(rule);

        recordOutboxEvent("RULE", rule.getPublicId(), "RULE_DELETED", ruleMapper.toResponse(rule), "promotion.campaign.lifecycle", deleter);
    }

    @Override
    @Transactional(readOnly = true)
    public RuleResponse getRule(String publicId) {
        PromotionRule rule = ruleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Rule not found", HttpStatus.NOT_FOUND));

        if (rule.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Rule has been deleted", HttpStatus.NOT_FOUND);
        }

        return ruleMapper.toResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RuleResponse> searchRules(String campaignPublicId, String code, Boolean enabled, Pageable pageable) {
        Specification<PromotionRule> spec = Specification.where(RuleSpecification.isNotDeleted())
                .and(RuleSpecification.hasCampaignPublicId(campaignPublicId))
                .and(RuleSpecification.hasCode(code))
                .and(RuleSpecification.isEnabled(enabled));

        Page<PromotionRule> page = ruleRepository.findAll(spec, pageable);

        List<RuleResponse> dtoList = page.getContent().stream()
                .map(ruleMapper::toResponse)
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
    public RuleResponse cloneRule(String publicId, RuleCloneRequest request, String creator) {
        PromotionRule source = ruleRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Source Rule not found", HttpStatus.NOT_FOUND));

        if (source.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Source Rule is deleted", HttpStatus.BAD_REQUEST);
        }

        String campaignId = request.getTargetCampaignPublicId() != null ? request.getTargetCampaignPublicId() : source.getCampaignPublicId();

        if (ruleRepository.existsByCodeAndCampaignPublicIdAndDeletedAtIsNull(request.getNewCode(), campaignId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "New Rule code already exists in target campaign", HttpStatus.BAD_REQUEST);
        }
        PromotionCampaign campaign = campaignRepository.findByPublicId(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Target Campaign not found", HttpStatus.BAD_REQUEST));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Target Campaign is deleted", HttpStatus.BAD_REQUEST);
        }

        PromotionRule cloned = new PromotionRule();
        cloned.setCampaignPublicId(campaignId);
        cloned.setCode(request.getNewCode());
        cloned.setName(request.getNewName());
        cloned.setDescription(source.getDescription());
        cloned.setRuleType(source.getRuleType());
        cloned.setPriority(source.getPriority());
        cloned.setExecutionOrder(source.getExecutionOrder());
        cloned.setStackable(source.getStackable());
        cloned.setStopFurtherRules(source.getStopFurtherRules());
        cloned.setEnabled(source.getEnabled());
        cloned.setConditionsJson(source.getConditionsJson());
        cloned.setActionsJson(source.getActionsJson());
        cloned.setMetadataJson(source.getMetadataJson());
        cloned.setEffectiveFrom(source.getEffectiveFrom());
        cloned.setEffectiveTo(source.getEffectiveTo());
        cloned.setCreatedBy(creator);
        cloned.setUpdatedBy(creator);

        PromotionRule saved = ruleRepository.save(cloned);

        recordOutboxEvent("RULE", saved.getPublicId(), "RULE_CLONED", ruleMapper.toResponse(saved), "promotion.campaign.lifecycle", creator);

        return ruleMapper.toResponse(saved);
    }

    @Override
    public boolean validateRuleJson(String conditionsJson, String actionsJson) {
        try {
            if (conditionsJson != null && !conditionsJson.isBlank()) {
                objectMapper.readTree(conditionsJson);
            }
            if (actionsJson != null && !actionsJson.isBlank()) {
                objectMapper.readTree(actionsJson);
            }
            return true;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Invalid JSON formatting in conditions or actions: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public double previewDiscount(String conditionsJson, String actionsJson, String contextJson) {
        validateRuleJson(conditionsJson, actionsJson);
        try {
            double orderAmount = 0.0;
            if (contextJson != null && !contextJson.isBlank()) {
                JsonNode contextNode = objectMapper.readTree(contextJson);
                if (contextNode.has("orderAmount")) {
                    orderAmount = contextNode.get("orderAmount").asDouble();
                }
            }

            JsonNode actionsNode = objectMapper.readTree(actionsJson);
            double discount = 0.0;
            if (actionsNode.has("discountType") && actionsNode.has("discountValue")) {
                String type = actionsNode.get("discountType").asText();
                double value = actionsNode.get("discountValue").asDouble();

                if ("PERCENTAGE".equalsIgnoreCase(type)) {
                    discount = orderAmount * (value / 100.0);
                    if (actionsNode.has("maxDiscountAmount")) {
                        double maxDiscount = actionsNode.get("maxDiscountAmount").asDouble();
                        if (discount > maxDiscount) {
                            discount = maxDiscount;
                        }
                    }
                } else if ("FIXED_AMOUNT".equalsIgnoreCase(type)) {
                    discount = value;
                }
            }

            return Math.min(discount, orderAmount);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Failed to preview discount due to invalid payload schemas: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void recordOutboxEvent(String aggregateType, String aggregatePublicId, String eventType, Object payload, String topic, String actor) {
        try {
            PromotionOutboxEvent event = new PromotionOutboxEvent();
            event.setAggregateType(aggregateType);
            event.setAggregatePublicId(aggregatePublicId);
            event.setEventType(eventType);
            event.setEventKey(aggregatePublicId);
            event.setPayload(objectMapper.writeValueAsString(payload));
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
