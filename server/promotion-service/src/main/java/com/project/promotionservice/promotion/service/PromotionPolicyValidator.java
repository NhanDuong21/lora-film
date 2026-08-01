package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class PromotionPolicyValidator {

    private static final BigDecimal MAX_LEGAL_PERCENTAGE = new BigDecimal("50");
    private final PromotionConditionEvaluator conditionEvaluator;

    public PromotionPolicyValidator(PromotionConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    public void validatePromotion(JsonNode conditions, JsonNode actions) {
        conditionEvaluator.validateConfiguration(conditions);
        requireSingleAction(actions);
    }

    private void requireSingleAction(JsonNode actions) {
        JsonNode action = actions;
        if (actions != null && actions.isArray()) {
            if (actions.size() != 1) {
                invalid("actionsJson must contain exactly one deterministic discount action");
            }
            action = actions.get(0);
        }
        if (action == null || !action.isObject() || action.isEmpty()) {
            invalid("actionsJson must define one discount action");
        }

        String type = text(action, "discountType", "type", "actionType");
        if (type == null) {
            invalid("Promotion action type is required");
        }
        String normalized = type.toUpperCase(Locale.ROOT);
        BigDecimal value = decimal(action, "discountValue", "value", "amount", "percentage");
        if (normalized.equals("PERCENTAGE") || normalized.equals("PERCENT")) {
            if (value == null || value.signum() <= 0) {
                invalid("Percentage discount must be greater than zero");
            }
            if (value.compareTo(MAX_LEGAL_PERCENTAGE) > 0) {
                invalid("Percentage discount cannot exceed the legal 50% limit");
            }
        } else if (normalized.equals("FIXED_AMOUNT")
                || normalized.equals("AMOUNT")) {
            if (value == null || value.signum() <= 0) {
                invalid("Fixed discount amount must be greater than zero");
            }
        } else if (!(normalized.equals("FREE")
                || normalized.equals("FULL_DISCOUNT"))) {
            invalid("Unsupported promotion action type: " + type);
        }

        BigDecimal maximum = decimal(
                action, "maxDiscountAmount", "maximumDiscountAmount", "maxAmount");
        if (maximum != null && maximum.signum() <= 0) {
            invalid("Maximum discount amount must be greater than zero");
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
            if (value == null || value.isNull()) {
                continue;
            }
            try {
                return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
            } catch (NumberFormatException ignored) {
                invalid(field + " must be numeric");
            }
        }
        return null;
    }

    private void invalid(String message) {
        throw new BusinessException(
                PromotionValidationErrorCode.PROMOTION_CONFIGURATION_INVALID,
                message,
                HttpStatus.BAD_REQUEST);
    }
}
