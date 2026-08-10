package com.project.promotionservice.integration.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class IntegrationEventProcessor {
    private final ObjectMapper objectMapper;
    private final PromotionReservationService reservationService;
    private final CacheManager cacheManager;

    public IntegrationEventProcessor(ObjectMapper objectMapper,
                                     PromotionReservationService reservationService,
                                     CacheManager cacheManager) {
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
        this.cacheManager = cacheManager;
    }

    public boolean process(PromotionIntegrationEvent event) {
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());
            JsonNode data = root.has("data") ? root.get("data") : root;
            String type = event.getEventType().toUpperCase();
            switch (type) {
                case "PAYMENT_SUCCEEDED", "PAYMENT_COMPLETED" -> confirm(event, data);
                case "PAYMENT_FAILED" -> release(event, data);
                case "BOOKING_CANCELLED", "BOOKING_ORDER_CANCELLED", "ORDER_CANCELLED" -> cancel(event, data);
                case "BOOKING_CREATED", "NOTIFICATION_DELIVERED",
                     "PAYMENT_REFUNDED", "MEMBERSHIP_UPDATED" -> invalidateRuntimeCaches();
                case "MOVIE_UPDATED", "SHOWTIME_UPDATED" -> invalidateRuntimeCaches();
                default -> {
                    // Unknown versioned events are persisted and ignored instead of
                    // being silently lost; an operator can inspect/reprocess them.
                    return true;
                }
            }
            return false;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid integration event payload", exception);
        }
    }

    private void confirm(PromotionIntegrationEvent event, JsonNode data) {
        String reservationId = text(data, "reservationPublicId", "reservationId");
        String paymentId = text(data, "paymentPublicId", "paymentId");
        require(reservationId, "reservationPublicId");
        require(paymentId, "paymentPublicId");
        ConfirmRequest request = new ConfirmRequest(paymentId);
        reservationService.confirm(reservationId, request, event.getEventId(), "PAYMENT_SERVICE");
    }

    private void release(PromotionIntegrationEvent event, JsonNode data) {
        String reservationId = text(data, "reservationPublicId", "reservationId");
        require(reservationId, "reservationPublicId");
        TransitionRequest request = new TransitionRequest(
                "Payment failed event " + event.getEventId());
        reservationService.release(reservationId, request, event.getEventId(), "PAYMENT_SERVICE");
    }

    private void cancel(PromotionIntegrationEvent event, JsonNode data) {
        String reservationId = text(data, "reservationPublicId", "reservationId");
        require(reservationId, "reservationPublicId");
        TransitionRequest request = new TransitionRequest(
                "Booking cancelled event " + event.getEventId());
        reservationService.release(reservationId, request, event.getEventId(), "BOOKING_SERVICE");
    }

    private void invalidateRuntimeCaches() {
        for (String name : new String[]{"promotions", "campaigns", "vouchers", "reservations"}) {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
    }

    private String text(JsonNode data, String... names) {
        for (String name : names) {
            JsonNode value = data.get(name);
            if (value != null && !value.isNull() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
