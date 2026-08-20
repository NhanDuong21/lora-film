package com.project.promotionservice.integration.inbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.ConfirmRequest;
import com.project.promotionservice.reservation.dto.request.ReservationRequests.TransitionRequest;
import com.project.promotionservice.reservation.enums.ReleaseReasonType;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.project.promotionservice.automation.service.PromotionAutomationService;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.enums.AutomationRunStatus;

@Service
public class IntegrationEventProcessor {
    private final ObjectMapper objectMapper;
    private final PromotionReservationService reservationService;
    private final CacheManager cacheManager;
    private PromotionAutomationService automationService;

    public IntegrationEventProcessor(ObjectMapper objectMapper,
                                     PromotionReservationService reservationService,
                                     CacheManager cacheManager) {
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
        this.cacheManager = cacheManager;
    }

    @Autowired(required = false)
    public void setAutomationService(PromotionAutomationService automationService) {
        this.automationService = automationService;
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
                case "BOOKING_CONFIRMED" -> firstConfirmedBooking(data);
                case "BOOKING_REFUNDED" -> refundedBooking(data);
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

    private void firstConfirmedBooking(JsonNode data) {
        if (automationService == null
                || !data.path("firstConfirmedBooking").asBoolean(false)
                || !data.path("ticketIssued").asBoolean(false)
                || !data.path("automationEligible").asBoolean(false)) {
            return;
        }
        String customerId = text(data, "automationCustomerId");
        String bookingReference = text(data, "publicId", "bookingPublicId");
        require(customerId, "automationCustomerId");
        require(bookingReference, "bookingPublicId");
        PromotionAutomationRun run = automationService.createSecondBookingRun(
                customerId, bookingReference);
        if (run.getStatus() == AutomationRunStatus.AUDIENCE_READY) {
            automationService.createIssueJob(run.getPublicId(), 200);
        }
    }

    private void refundedBooking(JsonNode data) {
        if (automationService == null) return;
        String bookingReference = text(data, "publicId", "bookingPublicId");
        if (bookingReference != null) {
            automationService.revokeSecondBookingForRefund(bookingReference);
        }
        invalidateRuntimeCaches();
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
                ReleaseReasonType.PAYMENT_FAILED,
                "Payment failed event " + event.getEventId(),
                "PAYMENT_SERVICE", event.getEventId(), null);
        reservationService.release(reservationId, request, event.getEventId(), "PAYMENT_SERVICE");
    }

    private void cancel(PromotionIntegrationEvent event, JsonNode data) {
        String reservationId = text(data, "reservationPublicId", "reservationId");
        require(reservationId, "reservationPublicId");
        TransitionRequest request = new TransitionRequest(
                ReleaseReasonType.CUSTOMER_CANCELLED_BOOKING,
                "Booking cancelled event " + event.getEventId(),
                "BOOKING_SERVICE", event.getEventId(), null);
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
