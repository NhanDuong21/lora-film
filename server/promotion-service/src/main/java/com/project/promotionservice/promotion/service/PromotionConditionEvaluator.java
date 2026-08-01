package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.promotionservice.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

@Component
public class PromotionConditionEvaluator {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "minimumOrderAmount",
            "minOrderAmount",
            "movieIds",
            "movieId",
            "moviePublicIds",
            "showtimeIds",
            "showtimePublicIds",
            "cinemaIds",
            "cinemaId",
            "cinemaPublicIds",
            "paymentMethods",
            "paymentMethod",
            "channels",
            "channel",
            "formats",
            "format",
            "orderTypes",
            "orderType",
            "allowedUserIds",
            "dayOfWeek",
            "purchaseDayOfWeek",
            "showtimeDayOfWeek",
            "seatTypes",
            "seatType",
            "excludeRoomTypes",
            "excludeRoomType",
            "excludeDates",
            "requiredTierCode",
            "requiresVerification",
            "legalDiscountCapExempt",
            "allowMultipleVoucherPerOrder",
            "stackableWith",
            "notStackableWith");

    public void validateConfiguration(JsonNode conditions) {
        if (conditions == null || !conditions.isObject()) {
            invalidConfiguration("conditionsJson must be a JSON object");
        }
        Iterator<String> names = conditions.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!SUPPORTED_FIELDS.contains(name)) {
                invalidConfiguration("Unsupported promotion condition: " + name);
            }
        }
        rejectAliasesTogether(conditions, "minimumOrderAmount", "minOrderAmount");
        rejectAliasesTogether(conditions, "movieIds", "movieId");
        rejectAliasesTogether(conditions, "cinemaIds", "cinemaId");
        rejectAliasesTogether(conditions, "paymentMethods", "paymentMethod");
        rejectAliasesTogether(conditions, "channels", "channel");
        rejectAliasesTogether(conditions, "formats", "format");
        rejectAliasesTogether(conditions, "orderTypes", "orderType");
        rejectAliasesTogether(conditions, "seatTypes", "seatType");
        rejectAliasesTogether(conditions, "excludeRoomTypes", "excludeRoomType");
        requireArrayWhenPresent(conditions, "movieIds");
        requireArrayWhenPresent(conditions, "movieId");
        requireArrayWhenPresent(conditions, "moviePublicIds");
        requireArrayWhenPresent(conditions, "showtimeIds");
        requireArrayWhenPresent(conditions, "showtimePublicIds");
        requireArrayWhenPresent(conditions, "cinemaIds");
        requireArrayWhenPresent(conditions, "cinemaId");
        requireArrayWhenPresent(conditions, "cinemaPublicIds");
        requireArrayWhenPresent(conditions, "paymentMethods");
        requireArrayWhenPresent(conditions, "paymentMethod");
        requireArrayWhenPresent(conditions, "channels");
        requireArrayWhenPresent(conditions, "channel");
        requireArrayWhenPresent(conditions, "formats");
        requireArrayWhenPresent(conditions, "format");
        requireArrayWhenPresent(conditions, "orderTypes");
        requireArrayWhenPresent(conditions, "orderType");
        requireArrayWhenPresent(conditions, "allowedUserIds");
        requireArrayWhenPresent(conditions, "dayOfWeek");
        requireArrayWhenPresent(conditions, "purchaseDayOfWeek");
        requireArrayWhenPresent(conditions, "showtimeDayOfWeek");
        requireArrayWhenPresent(conditions, "seatTypes");
        requireArrayWhenPresent(conditions, "seatType");
        requireArrayWhenPresent(conditions, "excludeRoomTypes");
        requireArrayWhenPresent(conditions, "excludeRoomType");
        requireArrayWhenPresent(conditions, "excludeDates");
        requireArrayWhenPresent(conditions, "stackableWith");
        requireArrayWhenPresent(conditions, "notStackableWith");
        validateMinimum(conditions);
        validateBooleanWhenPresent(conditions, "requiresVerification");
        validateBooleanWhenPresent(conditions, "legalDiscountCapExempt");
        validateBooleanWhenPresent(conditions, "allowMultipleVoucherPerOrder");
        validateTextWhenPresent(conditions, "requiredTierCode");
        validateDayValues(conditions.get("dayOfWeek"));
        validateDayValues(conditions.get("purchaseDayOfWeek"));
        validateDayValues(conditions.get("showtimeDayOfWeek"));
        validateDateValues(conditions.get("excludeDates"));
    }

    public void evaluate(JsonNode conditions, EvaluationContext request) {
        validateConfiguration(conditions);
        BigDecimal minimum = decimal(conditions, "minimumOrderAmount", "minOrderAmount");
        if (minimum != null && request.originalAmount().compareTo(minimum) < 0) {
            conditionNotMet("Minimum order amount is not met");
        }
        JsonNode context = request.contextJson();
        matchAllowed(conditions, context, "movieIds", "movieId");
        matchAllowed(conditions, context, "movieId", "movieId");
        matchAllowed(conditions, context, "moviePublicIds", "moviePublicId");
        matchAllowed(conditions, context, "showtimeIds", "showtimeId");
        matchAllowed(conditions, context, "showtimePublicIds", "showtimePublicId");
        matchAllowed(conditions, context, "cinemaIds", "cinemaId");
        matchAllowed(conditions, context, "cinemaId", "cinemaId");
        matchAllowed(conditions, context, "cinemaPublicIds", "cinemaPublicId");
        matchAllowed(conditions, context, "paymentMethods", "paymentMethod");
        matchAllowed(conditions, context, "paymentMethod", "paymentMethod");
        matchAllowed(conditions, context, "channels", "channel");
        matchAllowed(conditions, context, "channel", "channel");
        matchAllowed(conditions, context, "formats", "format");
        matchAllowed(conditions, context, "format", "format");
        matchAllowed(conditions, context, "orderTypes", "orderType");
        matchAllowed(conditions, context, "orderType", "orderType");
        matchAllowed(conditions, context, "seatTypes", "seatTypes");
        matchAllowed(conditions, context, "seatType", "seatTypes");

        JsonNode allowedUsers = conditions.get("allowedUserIds");
        if (isConfiguredArray(allowedUsers)
                && !arrayContains(allowedUsers, request.userPublicId())) {
            conditionNotMet("Customer is not eligible for this benefit");
        }
        evaluateDayOfWeek(
                conditions.get("dayOfWeek"), context, "dayOfWeek", "businessDate");
        evaluateDayOfWeek(
                conditions.get("purchaseDayOfWeek"), context,
                "purchaseDayOfWeek", "purchaseDate");
        evaluateDayOfWeek(
                conditions.get("showtimeDayOfWeek"), context,
                "showtimeDayOfWeek", "showtimeDate");
        JsonNode excludedRoomTypes = conditions.has("excludeRoomTypes")
                ? conditions.get("excludeRoomTypes") : conditions.get("excludeRoomType");
        evaluateExcludedRoomType(excludedRoomTypes, context);
        evaluateExcludedDates(conditions.get("excludeDates"), context);
        evaluateTier(conditions.get("requiredTierCode"), context);
        if (conditions.path("requiresVerification").asBoolean(false)
                && (context == null || !context.path("identityVerified").asBoolean(false))) {
            conditionNotMet("Verified customer eligibility is required");
        }
    }

    public record EvaluationContext(
            BigDecimal originalAmount,
            String userPublicId,
            JsonNode contextJson) {
    }

    private void evaluateDayOfWeek(
            JsonNode allowedDays, JsonNode context,
            String dayField, String dateField) {
        if (!isConfiguredArray(allowedDays)) {
            return;
        }
        String dayValue = contextValue(context, dayField);
        if (dayValue == null) {
            String dateValue = contextValue(context, dateField);
            if (dateValue != null) {
                try {
                    dayValue = LocalDate.parse(dateValue).getDayOfWeek().name();
                } catch (RuntimeException exception) {
                    conditionNotMet(dateField + " must use ISO-8601 yyyy-MM-dd");
                }
            }
        }
        if (dayValue == null || !arrayContains(allowedDays, dayValue)) {
            conditionNotMet("Promotion is not available on this day of week");
        }
        try {
            DayOfWeek.valueOf(dayValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            conditionNotMet("dayOfWeek is invalid");
        }
    }

    private void evaluateExcludedRoomType(JsonNode excluded, JsonNode context) {
        if (!isConfiguredArray(excluded)) {
            return;
        }
        String roomType = contextValue(context, "roomType");
        if (roomType == null) {
            roomType = contextValue(context, "format");
        }
        if (roomType != null && arrayContains(excluded, roomType)) {
            conditionNotMet("Room type is excluded from this promotion");
        }
    }

    private void evaluateExcludedDates(JsonNode excluded, JsonNode context) {
        if (!isConfiguredArray(excluded)) {
            return;
        }
        String businessDate = contextValue(context, "businessDate");
        if (businessDate == null) {
            conditionNotMet("businessDate is required for exclusion-date validation");
        }
        try {
            LocalDate.parse(businessDate);
        } catch (RuntimeException exception) {
            conditionNotMet("businessDate must use ISO-8601 yyyy-MM-dd");
        }
        if (arrayContains(excluded, businessDate)) {
            conditionNotMet("Promotion is excluded on this business date");
        }
    }

    private void evaluateTier(JsonNode requiredTier, JsonNode context) {
        if (requiredTier == null || requiredTier.isNull()
                || requiredTier.asText().isBlank()) {
            return;
        }
        String verifiedTier = contextValue(context, "verifiedTierCode");
        if (verifiedTier == null
                || !requiredTier.asText().equalsIgnoreCase(verifiedTier)) {
            conditionNotMet("Verified membership tier is not eligible");
        }
    }

    private void matchAllowed(
            JsonNode conditions,
            JsonNode context,
            String conditionField,
            String contextField) {
        JsonNode allowed = conditions.get(conditionField);
        if (!isConfiguredArray(allowed)) {
            return;
        }
        JsonNode actual = context == null ? null : context.get(contextField);
        if ((actual == null || actual.isNull()) && contextField.endsWith("s")) {
            actual = context == null
                    ? null : context.get(contextField.substring(0, contextField.length() - 1));
        }
        if (actual == null || actual.isNull()) {
            conditionNotMet("Condition not met: " + contextField);
        }
        if (actual.isArray()) {
            if (actual.isEmpty()) {
                conditionNotMet("Condition not met: " + contextField);
            }
            for (JsonNode value : actual) {
                if (!arrayContains(allowed, value.asText())) {
                    conditionNotMet("Condition not met: " + contextField);
                }
            }
            return;
        }
        if (actual.asText().isBlank() || !arrayContains(allowed, actual.asText())) {
            conditionNotMet("Condition not met: " + contextField);
        }
    }

    private String contextValue(JsonNode context, String field) {
        if (context == null || !context.isObject()) {
            return null;
        }
        JsonNode value = context.get(field);
        return value == null || value.isNull() || value.asText().isBlank()
                ? null : value.asText();
    }

    private boolean arrayContains(JsonNode values, String expected) {
        for (JsonNode value : values) {
            if (value.asText().equalsIgnoreCase(expected)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConfiguredArray(JsonNode node) {
        return node != null && node.isArray() && !node.isEmpty();
    }

    private void requireArrayWhenPresent(JsonNode conditions, String field) {
        JsonNode value = conditions.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isArray() || value.isEmpty()) {
            invalidConfiguration(field + " must be a non-empty array");
        }
        for (JsonNode item : value) {
            if (!item.isTextual() || item.asText().isBlank()) {
                invalidConfiguration(field + " must contain only non-blank strings");
            }
        }
    }

    private void rejectAliasesTogether(JsonNode conditions, String first, String second) {
        if (conditions.hasNonNull(first) && conditions.hasNonNull(second)) {
            invalidConfiguration("Use only one of " + first + " or " + second);
        }
    }

    private void validateMinimum(JsonNode conditions) {
        BigDecimal minimum = decimal(conditions, "minimumOrderAmount", "minOrderAmount");
        if (minimum != null && minimum.signum() < 0) {
            invalidConfiguration("Minimum order amount cannot be negative");
        }
    }

    private void validateBooleanWhenPresent(JsonNode conditions, String field) {
        JsonNode value = conditions.get(field);
        if (value != null && !value.isNull() && !value.isBoolean()) {
            invalidConfiguration(field + " must be boolean");
        }
    }

    private void validateTextWhenPresent(JsonNode conditions, String field) {
        JsonNode value = conditions.get(field);
        if (value != null && !value.isNull()
                && (!value.isTextual() || value.asText().isBlank())) {
            invalidConfiguration(field + " must be a non-blank string");
        }
    }

    private void validateDayValues(JsonNode values) {
        if (values == null || values.isNull()) {
            return;
        }
        for (JsonNode value : values) {
            try {
                DayOfWeek.valueOf(value.asText().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                invalidConfiguration("dayOfWeek contains an invalid value: " + value.asText());
            }
        }
    }

    private void validateDateValues(JsonNode values) {
        if (values == null || values.isNull()) {
            return;
        }
        for (JsonNode value : values) {
            try {
                LocalDate.parse(value.asText());
            } catch (RuntimeException exception) {
                invalidConfiguration("excludeDates must contain ISO-8601 yyyy-MM-dd values");
            }
        }
    }

    private BigDecimal decimal(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            try {
                return value.isNumber()
                        ? value.decimalValue()
                        : new BigDecimal(value.asText());
            } catch (NumberFormatException exception) {
                invalidConfiguration(field + " must be numeric");
            }
        }
        return null;
    }

    private void conditionNotMet(String message) {
        throw new BusinessException(
                PromotionValidationErrorCode.PROMOTION_CONDITION_NOT_MET,
                message,
                HttpStatus.BAD_REQUEST);
    }

    private void invalidConfiguration(String message) {
        throw new BusinessException(
                PromotionValidationErrorCode.PROMOTION_CONFIGURATION_INVALID,
                message,
                HttpStatus.BAD_REQUEST);
    }
}
