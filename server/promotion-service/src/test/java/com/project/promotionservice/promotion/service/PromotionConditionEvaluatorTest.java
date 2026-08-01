package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class PromotionConditionEvaluatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromotionConditionEvaluator evaluator = new PromotionConditionEvaluator();

    @Test
    void acceptsSelectedMovieAndCinemaPublicIdsFromCheckoutContext() {
        var conditions = objectMapper.createObjectNode();
        conditions.putArray("moviePublicIds").add("movie-public-1");
        conditions.putArray("cinemaPublicIds").add("cinema-public-1");
        var context = objectMapper.createObjectNode();
        context.put("moviePublicId", "movie-public-1");
        context.put("cinemaPublicId", "cinema-public-1");

        assertThatCode(() -> evaluator.evaluate(conditions,
                new PromotionConditionEvaluator.EvaluationContext(
                        new BigDecimal("150000"), "42", context)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnselectedCinemaPublicId() {
        var conditions = objectMapper.createObjectNode();
        conditions.putArray("cinemaPublicIds").add("cinema-public-1");
        var context = objectMapper.createObjectNode();
        context.put("cinemaPublicId", "cinema-public-2");

        assertThatThrownBy(() -> evaluator.evaluate(conditions,
                new PromotionConditionEvaluator.EvaluationContext(
                        new BigDecimal("150000"), "42", context)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cinemaPublicId");
    }

    @Test
    void acceptsSelectedPaymentMethodFromCheckoutContext() {
        var conditions = objectMapper.createObjectNode();
        conditions.putArray("paymentMethods").add("VNPAY");
        var context = objectMapper.createObjectNode();
        context.put("paymentMethod", "vnpay");

        assertThatCode(() -> evaluator.evaluate(conditions,
                new PromotionConditionEvaluator.EvaluationContext(
                        new BigDecimal("150000"), "42", context)))
                .doesNotThrowAnyException();
    }
}
