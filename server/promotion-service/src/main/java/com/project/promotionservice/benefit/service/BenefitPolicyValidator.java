package com.project.promotionservice.benefit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponType;
import com.project.promotionservice.benefit.enums.BenefitEnums.DistributionType;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherType;
import com.project.promotionservice.benefit.exception.BenefitErrorCode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class BenefitPolicyValidator {

    private static final BigDecimal MAX_LEGAL_PERCENTAGE = new BigDecimal("50");

    public void validateCoupon(
            CouponType couponType, DistributionType distributionType,
            Boolean reusable, Integer maxRedemptions, Integer maxRedemptionsPerUser,
            JsonNode conditions, JsonNode actions) {
        requireObject("conditionsJson", conditions);
        requireSingleAction(actions);

        if (distributionType == DistributionType.PRIVATE
                || distributionType == DistributionType.TARGETED
                || couponType == CouponType.PRIVATE) {
            JsonNode allowedUsers = conditions.get("allowedUserIds");
            if (allowedUsers == null || !allowedUsers.isArray() || allowedUsers.isEmpty()) {
                invalid("Private or targeted coupon requires a non-empty allowedUserIds list");
            }
        }
        if (couponType == CouponType.SINGLE_USE
                && (!Integer.valueOf(1).equals(maxRedemptions)
                || !Integer.valueOf(1).equals(maxRedemptionsPerUser)
                || Boolean.TRUE.equals(reusable))) {
            invalid("SINGLE_USE coupon must have maxRedemptions=1, maxRedemptionsPerUser=1 and reusable=false");
        }
        if (maxRedemptions != null && maxRedemptionsPerUser != null
                && maxRedemptionsPerUser > maxRedemptions) {
            invalid("maxRedemptionsPerUser cannot exceed maxRedemptions");
        }
        if (!Boolean.TRUE.equals(reusable)
                && maxRedemptionsPerUser != null && maxRedemptionsPerUser > 1) {
            invalid("Non-reusable coupon cannot be used more than once per user");
        }
    }

    public void validateVoucher(
            VoucherType voucherType, Boolean reusable, Integer maxUsage,
            BigDecimal faceValue, JsonNode conditions, JsonNode actions) {
        requireObject("conditionsJson", conditions);
        requireSingleAction(actions);
        if (!Boolean.TRUE.equals(reusable) && maxUsage != null && maxUsage > 1) {
            invalid("Non-reusable voucher must have maxUsage=1");
        }
        if (voucherType == VoucherType.FIXED_AMOUNT
                && (faceValue == null || faceValue.signum() <= 0)) {
            invalid("FIXED_AMOUNT voucher requires a positive faceValue");
        }
    }

    private void requireObject(String field, JsonNode value) {
        if (value == null || !value.isObject()) {
            invalid(field + " must be a JSON object");
        }
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
            invalid("Benefit action type is required");
        }
        String normalized = type.toUpperCase(Locale.ROOT);
        BigDecimal value = decimal(action, "discountValue", "value", "amount", "percentage");
        if (normalized.contains("PERCENT")) {
            if (value == null || value.signum() <= 0) {
                invalid("Percentage discount must be greater than zero");
            }
            if (value.compareTo(MAX_LEGAL_PERCENTAGE) > 0) {
                invalid("Percentage discount cannot exceed the legal 50% limit");
            }
        } else if (normalized.contains("FIXED")
                || normalized.contains("AMOUNT")
                || normalized.equals("CASHBACK")) {
            if (value == null || value.signum() <= 0) {
                invalid("Fixed discount amount must be greater than zero");
            }
        } else if (!(normalized.equals("FREE")
                || normalized.equals("FREE_TICKET")
                || normalized.equals("FREE_COMBO")
                || normalized.equals("FULL_DISCOUNT"))) {
            invalid("Unsupported benefit action type: " + type);
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
                BenefitErrorCode.BENEFIT_CONFIGURATION_INVALID,
                message,
                HttpStatus.BAD_REQUEST);
    }
}
