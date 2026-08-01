package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromotionDiscountCalculatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PromotionDiscountCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PromotionDiscountCalculator();
    }

    @Test
    void fullDiscountMakesTheEligibleOrderFree() throws Exception {
        BigDecimal discount = calculator.calculate(
                objectMapper.readTree("{\"discountType\":\"FULL_DISCOUNT\"}"),
                new BigDecimal("285000"),
                objectMapper.createObjectNode());

        assertThat(discount).isEqualByComparingTo("285000.00");
    }

    @Test
    void percentageDiscountHonorsTheConfiguredCap() throws Exception {
        BigDecimal discount = calculator.calculate(
                objectMapper.readTree("""
                        {"discountType":"PERCENTAGE","discountValue":20,"maxDiscountAmount":30000}
                        """),
                new BigDecimal("285000"),
                objectMapper.createObjectNode());

        assertThat(discount).isEqualByComparingTo("30000.00");
    }

    @Test
    void fixedDiscountNeverExceedsTheOrderAmount() throws Exception {
        BigDecimal discount = calculator.calculate(
                objectMapper.readTree(
                        "{\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":500000}"),
                new BigDecimal("285000"),
                objectMapper.createObjectNode());

        assertThat(discount).isEqualByComparingTo("285000.00");
    }

    @Test
    void percentageAboveBusinessLimitIsRejected() throws Exception {
        var action = objectMapper.readTree(
                "{\"discountType\":\"PERCENTAGE\",\"discountValue\":51}");

        assertThatThrownBy(() -> calculator.calculate(
                action, new BigDecimal("285000"), objectMapper.createObjectNode()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("between 0 and 50");
    }
}
