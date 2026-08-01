package com.lorafilm.booking.booking.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.dto.response.PromotionQuoteResponse;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class PromotionServiceReservationClient implements PromotionReservationClient {

    private static final String SERVICE_NAME = "BOOKING_SERVICE";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalToken;

    public PromotionServiceReservationClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${services.promotion-service.url:http://localhost:8087}") String serviceUrl,
            @Value("${services.promotion-service.internal-token:${app.internal-token}}") String internalToken) {
        this.restClient = restClientBuilder.baseUrl(serviceUrl).build();
        this.objectMapper = objectMapper;
        this.internalToken = internalToken;
    }

    @Override
    public PromotionQuoteResponse preview(CheckoutCommand command) {
        return requireData(post("/internal/runtime/preview", command, null, QuoteEnvelope.class));
    }

    @Override
    public ReservationResult reserve(CheckoutCommand command, String idempotencyKey) {
        return requireData(post("/internal/reservations", command, idempotencyKey, ReservationEnvelope.class));
    }

    @Override
    public void confirm(String reservationPublicId, String paymentPublicId, String idempotencyKey) {
        post("/internal/reservations/" + reservationPublicId + "/confirm",
                Map.of("paymentPublicId", paymentPublicId), idempotencyKey, ReservationEnvelope.class);
    }

    @Override
    public void release(String reservationPublicId, String reason, String idempotencyKey) {
        post("/internal/reservations/" + reservationPublicId + "/release",
                Map.of("reason", reason), idempotencyKey, ReservationEnvelope.class);
    }

    private <T> T post(String path, Object body, String idempotencyKey, Class<T> responseType) {
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(path)
                    .header("X-Service-Name", SERVICE_NAME)
                    .header("X-Internal-Token", internalToken);
            if (idempotencyKey != null) {
                request.header("X-Idempotency-Key", idempotencyKey);
            }
            return request.body(body).retrieve().body(responseType);
        } catch (RestClientResponseException exception) {
            throw mapResponseException(exception);
        } catch (RestClientException exception) {
            throw new IntegrationException("Cannot connect to Promotion Service", exception);
        }
    }

    private RuntimeException mapResponseException(RestClientResponseException exception) {
        try {
            JsonNode error = objectMapper.readTree(exception.getResponseBodyAsString());
            String code = error.path("errorCode").asText("PROMOTION_REJECTED");
            String message = error.path("message").asText("Khuyến mãi không thể áp dụng cho đơn này");
            HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
            return new BusinessException(code, message,
                    status == null ? HttpStatus.BAD_GATEWAY : status);
        } catch (Exception ignored) {
            return new IntegrationException("Promotion Service returned an invalid error response", exception);
        }
    }

    private PromotionQuoteResponse requireData(QuoteEnvelope envelope) {
        if (envelope == null || !envelope.success() || envelope.data() == null) {
            throw new IntegrationException("Promotion Service returned an invalid preview response");
        }
        return envelope.data();
    }

    private ReservationResult requireData(ReservationEnvelope envelope) {
        if (envelope == null || !envelope.success() || envelope.data() == null) {
            throw new IntegrationException("Promotion Service returned an invalid reservation response");
        }
        return envelope.data();
    }

    private record QuoteEnvelope(boolean success, String message, PromotionQuoteResponse data) {
    }

    private record ReservationEnvelope(boolean success, String message, ReservationResult data) {
    }
}
