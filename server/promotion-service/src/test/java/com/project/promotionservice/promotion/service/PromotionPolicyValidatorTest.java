package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionPolicyValidatorTest {

    private ObjectMapper objectMapper;
    private PromotionPolicyValidator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new PromotionPolicyValidator(new PromotionConditionEvaluator());
    }

    @Test
    void acceptsFullPercentageAndFixedDiscountsLargerThanAnOrder() throws Exception {
        assertThatCode(() -> validator.validatePromotion(
                objectMapper.createObjectNode(),
                objectMapper.readTree(
                        "{\"discountType\":\"PERCENTAGE\",\"discountValue\":100}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validatePromotion(
                objectMapper.createObjectNode(),
                objectMapper.readTree(
                        "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":15000}")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPercentageValuesAboveOneHundred() throws Exception {
        assertThatThrownBy(() -> validator.validatePromotion(
                objectMapper.createObjectNode(),
                objectMapper.readTree(
                        "{\"discountType\":\"PERCENTAGE\",\"discountValue\":101}")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot exceed 100%");
    }
}
