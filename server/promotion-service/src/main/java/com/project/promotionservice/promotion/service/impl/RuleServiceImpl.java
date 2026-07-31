package com.project.promotionservice.promotion.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.benefit.service.BenefitPolicyValidator;
import com.project.promotionservice.benefit.service.BenefitConditionEvaluator;
import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitValidationRequest;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEvent;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import com.project.promotionservice.integration.outbox.PromotionOutboxEnvelopeFactory;
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
import com.project.promotionservice.promotion.service.CampaignConfigurationPolicy;
import com.project.promotionservice.promotion.service.RuleService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.List;

@Service
public class RuleServiceImpl implements RuleService {

    private final PromotionRuleRepository ruleRepository;
    private final PromotionCampaignRepository campaignRepository;
    private final PromotionOutboxEventRepository outboxEventRepository;
    private final RuleMapper ruleMapper;
    private final ObjectMapper objectMapper;
    private final BenefitPolicyValidator benefitPolicyValidator;
    private final BenefitConditionEvaluator conditionEvaluator;
    private final PromotionOutboxEnvelopeFactory envelopeFactory;
    private final CampaignConfigurationPolicy configurationPolicy;

    public RuleServiceImpl(PromotionRuleRepository ruleRepository,
                           PromotionCampaignRepository campaignRepository,
                           PromotionOutboxEventRepository outboxEventRepository,
                           RuleMapper ruleMapper,
                           ObjectMapper objectMapper,
                           BenefitPolicyValidator benefitPolicyValidator,
                           BenefitConditionEvaluator conditionEvaluator,
                           PromotionOutboxEnvelopeFactory envelopeFactory,
                           CampaignConfigurationPolicy configurationPolicy) {
        this.ruleRepository = ruleRepository;
        this.campaignRepository = campaignRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.ruleMapper = ruleMapper;
        this.objectMapper = objectMapper;
        this.benefitPolicyValidator = benefitPolicyValidator;
        this.conditionEvaluator = conditionEvaluator;
        this.envelopeFactory = envelopeFactory;
        this.configurationPolicy = configurationPolicy;
    }

    @Override
    @Transactional
    public RuleResponse createRule(RuleCreateRequest request, String creator) {
        PromotionCampaign campaign = campaignRepository.findByPublicId(request.getCampaignPublicId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.BAD_REQUEST));

        if (campaign.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Campaign is deleted", HttpStatus.BAD_REQUEST);
        }
        configurationPolicy.requireEditable(campaign);

        if (ruleRepository.existsByCodeAndCampaignPublicIdAndDeletedAtIsNull(request.getCode(), request.getCampaignPublicId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Rule code already exists in this campaign", HttpStatus.BAD_REQUEST);
        }

        validateRuleJson(request.getConditionsJson(), request.getActionsJson());
        requireEffectivePeriod(request.getEffectiveFrom(), request.getEffectiveTo());

        PromotionRule rule = ruleMapper.toEntity(request);
        rule.setCreatedBy(creator);
        rule.setUpdatedBy(creator);

        PromotionRule saved = ruleRepository.save(rule);
        markCampaignChanged(campaign, creator);

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
        PromotionCampaign campaign = requireRuleCampaign(rule);
        configurationPolicy.requireEditable(campaign);

        validateRuleJson(request.getConditionsJson(), request.getActionsJson());
        requireEffectivePeriod(request.getEffectiveFrom(), request.getEffectiveTo());

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
        markCampaignChanged(campaign, updater);

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
        PromotionCampaign campaign = requireRuleCampaign(rule);
        configurationPolicy.requireEditable(campaign);

        rule.setDeletedAt(Instant.now());
        rule.setDeletedBy(deleter);
        ruleRepository.save(rule);
        markCampaignChanged(campaign, deleter);

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
        configurationPolicy.requireEditable(campaign);
        validateRuleJson(source.getConditionsJson(), source.getActionsJson());
        requireEffectivePeriod(source.getEffectiveFrom(), source.getEffectiveTo());

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
        markCampaignChanged(campaign, creator);

        recordOutboxEvent("RULE", saved.getPublicId(), "RULE_CLONED", ruleMapper.toResponse(saved), "promotion.campaign.lifecycle", creator);

        return ruleMapper.toResponse(saved);
    }

    @Override
    public boolean validateRuleJson(String conditionsJson, String actionsJson) {
        try {
            JsonNode conditions = conditionsJson == null || conditionsJson.isBlank()
                    ? null : objectMapper.readTree(conditionsJson);
            JsonNode actions = actionsJson == null || actionsJson.isBlank()
                    ? null : objectMapper.readTree(actionsJson);
            benefitPolicyValidator.validateRule(conditions, actions);
            return true;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Invalid JSON formatting in conditions or actions: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public double previewDiscount(String conditionsJson, String actionsJson, String contextJson) {
        validateRuleJson(conditionsJson, actionsJson);
        try {
            JsonNode context = contextJson == null || contextJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(contextJson);
            if (!context.isObject()) {
                throw invalidRule("contextJson must be a JSON object");
            }
            BigDecimal orderAmount = decimal(context, "orderAmount");
            if (orderAmount == null || orderAmount.signum() <= 0) {
                throw invalidRule("contextJson.orderAmount must be greater than zero");
            }
            BenefitValidationRequest request = new BenefitValidationRequest();
            request.setCode("RULE_PREVIEW");
            request.setUserPublicId(context.path("userPublicId").asText("RULE_PREVIEW"));
            request.setOriginalAmount(orderAmount);
            request.setContextJson(context);
            conditionEvaluator.evaluate(objectMapper.readTree(conditionsJson), request);

            JsonNode actions = objectMapper.readTree(actionsJson);
            JsonNode action = actions.isArray() ? actions.get(0) : actions;
            String type = text(action, "discountType", "type", "actionType");
            BigDecimal value = decimal(
                    action, "discountValue", "value", "amount", "percentage");
            BigDecimal discount;
            String normalized = type.toUpperCase(Locale.ROOT);
            if (normalized.equals("PERCENTAGE") || normalized.equals("PERCENT")) {
                discount = orderAmount.multiply(value)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal maximum = decimal(
                        action, "maxDiscountAmount", "maximumDiscountAmount", "maxAmount");
                if (maximum != null) {
                    discount = discount.min(maximum);
                }
            } else if (normalized.equals("FREE")
                    || normalized.equals("FULL_DISCOUNT")) {
                discount = orderAmount;
            } else if (normalized.equals("FREE_TICKET")) {
                discount = requirePreviewEligibleAmount(
                        context, "ticketAmount", orderAmount);
            } else if (normalized.equals("FREE_COMBO")) {
                discount = requirePreviewEligibleAmount(
                        context, "comboAmount", orderAmount);
            } else {
                discount = value;
            }
            return discount.min(orderAmount)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "Failed to preview discount due to invalid payload schemas: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private PromotionCampaign requireRuleCampaign(PromotionRule rule) {
        return campaignRepository.findByPublicId(rule.getCampaignPublicId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND, "Campaign not found", HttpStatus.NOT_FOUND));
    }

    private void markCampaignChanged(PromotionCampaign campaign, String actor) {
        configurationPolicy.markConfigurationChanged(campaign, actor);
        campaignRepository.save(campaign);
    }

    private void requireEffectivePeriod(Instant from, Instant to) {
        if (to != null && !to.isAfter(from)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PARAMETER,
                    "effectiveTo must be after effectiveFrom",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.isNumber()
                        ? value.decimalValue()
                        : new BigDecimal(value.asText());
            }
        }
        return null;
    }

    private BusinessException invalidRule(String message) {
        return new BusinessException(
                ErrorCode.INVALID_REQUEST_PARAMETER, message, HttpStatus.BAD_REQUEST);
    }

    private BigDecimal requirePreviewEligibleAmount(
            JsonNode context, String field, BigDecimal orderAmount) {
        BigDecimal amount = decimal(context, field);
        if (amount == null || amount.signum() <= 0 || amount.compareTo(orderAmount) > 0) {
            throw invalidRule(field + " must be greater than zero and not exceed orderAmount");
        }
        return amount;
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
