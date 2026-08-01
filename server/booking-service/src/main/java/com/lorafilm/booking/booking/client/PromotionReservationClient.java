package com.lorafilm.booking.booking.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.lorafilm.booking.booking.dto.response.PromotionQuoteResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PromotionReservationClient {

    PromotionQuoteResponse preview(CheckoutCommand command);

    ReservationResult reserve(CheckoutCommand command, String idempotencyKey);

    void confirm(String reservationPublicId, String paymentPublicId, String idempotencyKey);

    void release(String reservationPublicId, String reason, String idempotencyKey);

    record CheckoutCommand(
            String userPublicId,
            BigDecimal originalAmount,
            List<String> selectedUserPromotionPublicIds,
            List<String> selectedPromotionPublicIds,
            String couponCode,
            String bookingPublicId,
            String currency,
            JsonNode contextJson,
            Integer holdDurationSeconds,
            List<String> evaluationUserPromotionPublicIds,
            List<String> evaluationPromotionPublicIds) {
    }

    record ReservationResult(
            String publicId,
            String status,
            BigDecimal originalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String currency,
            Instant reservationExpiredAt,
            List<PromotionQuoteResponse.AppliedPromotion> appliedPromotions) {
    }
}
