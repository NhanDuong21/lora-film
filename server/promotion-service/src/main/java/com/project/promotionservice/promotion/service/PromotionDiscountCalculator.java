package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Component
public class PromotionDiscountCalculator {

    public BigDecimal calculate(
            JsonNode actions, BigDecimal originalAmount, JsonNode context) {
        JsonNode action = actions;
        if (actions != null && actions.isArray() && !actions.isEmpty()) {
            action = actions.get(0);
        }
        if (action == null || !action.isObject()) {
            throw invalid("Promotion action is not configured");
        }
        String type = text(action, "discountType", "type", "actionType");
        String normalized = type == null ? "" : type.toUpperCase(Locale.ROOT);
        BigDecimal value = decimal(
                action, "discountValue", "value", "amount", "percentage");
        BigDecimal discount;
        if (normalized.equals("PERCENTAGE") || normalized.equals("PERCENT")) {
            if (value == null || value.signum() <= 0
                    || value.compareTo(BigDecimal.valueOf(50)) > 0) {
                throw invalid("Percentage discount must be between 0 and 50");
            }
            discount = originalAmount.multiply(value)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal maximum = decimal(
                    action, "maxDiscountAmount", "maximumDiscountAmount", "maxAmount");
            if (maximum != null) {
                discount = discount.min(maximum);
            }
        } else if (normalized.equals("FREE") || normalized.equals("FULL_DISCOUNT")) {
            discount = originalAmount;
        } else if (normalized.equals("FIXED_AMOUNT")
                || normalized.equals("AMOUNT")) {
            if (value == null || value.signum() <= 0) {
                throw invalid("Fixed discount value must be greater than zero");
            }
            discount = value;
        } else {
            throw invalid("Unsupported promotion action type: " + normalized);
        }
        return money(discount.min(originalAmount));
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
                return decimalValue(value, field);
            }
        }
        return null;
    }

    private BigDecimal decimalValue(JsonNode value, String field) {
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return value.isNumber()
                    ? value.decimalValue() : new BigDecimal(value.asText());
        } catch (NumberFormatException exception) {
            throw invalid(field + " must be numeric");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException invalid(String message) {
        return new BusinessException(
                "PROMOTION_ACTION_INVALID", message, HttpStatus.BAD_REQUEST);
    }
}
