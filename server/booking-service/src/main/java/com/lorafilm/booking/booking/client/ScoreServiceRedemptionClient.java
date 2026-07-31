package com.lorafilm.booking.booking.client;

import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.common.exception.IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

@Component
public class ScoreServiceRedemptionClient implements ScoreRedemptionClient {

    private final RestClient restClient;
    private final String internalToken;

    public ScoreServiceRedemptionClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.score-service.url:http://localhost:8088}") String scoreServiceUrl,
            @Value("${services.score-service.internal-token:${app.internal-token}}") String internalToken) {
        this.restClient = restClientBuilder.baseUrl(scoreServiceUrl).build();
        this.internalToken = internalToken;
    }

    @Override
    public ScoreHoldResult hold(
            Long userId,
            Long bookingId,
            int points,
            int ttlSeconds,
            BigDecimal bookingAmount,
            String eventId,
            String idempotencyKey) {
        ScoreHoldEnvelope envelope;
        try {
            envelope = restClient.post()
                    .uri("/internal/scores/hold")
                    .header("X-Internal-Token", internalToken)
                    .body(new HoldRequest(
                            userId,
                            bookingId,
                            points,
                            ttlSeconds,
                            eventId,
                            idempotencyKey,
                            bookingAmount))
                    .retrieve()
                    .body(ScoreHoldEnvelope.class);
        } catch (RestClientResponseException exception) {
            throw mapResponseException(exception);
        } catch (RestClientException exception) {
            throw new IntegrationException("Cannot connect to Score Service", exception);
        }
        ScoreHoldPayload data = requireData(envelope, "hold");
        return new ScoreHoldResult(
                data.holdCode(),
                data.pointsHeld(),
                data.status(),
                data.discountAmount(),
                data.valuePerPoint(),
                data.idempotent());
    }

    @Override
    public void commit(Long bookingId, String holdCode, String eventId, String idempotencyKey) {
        post(
                "/internal/scores/commit",
                new CommitRequest(bookingId, holdCode, eventId, idempotencyKey));
    }

    @Override
    public void release(
            Long bookingId,
            String holdCode,
            String reason,
            String eventId,
            String idempotencyKey) {
        post(
                "/internal/scores/release",
                new ReleaseRequest(bookingId, holdCode, reason, eventId, idempotencyKey));
    }

    @Override
    public void refund(
            Long userId,
            Long bookingId,
            int points,
            String reason,
            String eventId,
            String idempotencyKey) {
        post(
                "/internal/scores/refund-redeem",
                new RefundRequest(userId, bookingId, points, null, eventId, idempotencyKey, reason));
    }

    private void post(String path, Object body) {
        try {
            restClient.post()
                    .uri(path)
                    .header("X-Internal-Token", internalToken)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw mapResponseException(exception);
        } catch (RestClientException exception) {
            throw new IntegrationException("Cannot connect to Score Service", exception);
        }
    }

    private RuntimeException mapResponseException(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status != null && status.is4xxClientError()) {
            return new BusinessException(
                    "SCORE_REDEMPTION_REJECTED",
                    "Không thể sử dụng điểm cho đơn này. Vui lòng kiểm tra lại số điểm khả dụng.",
                    status);
        }
        return new IntegrationException("Score Service is unavailable", exception);
    }

    private ScoreHoldPayload requireData(ScoreHoldEnvelope envelope, String operation) {
        if (envelope == null || !envelope.success() || envelope.data() == null) {
            throw new IntegrationException("Score Service returned an invalid " + operation + " response");
        }
        return envelope.data();
    }

    private record HoldRequest(
            Long userId,
            Long bookingId,
            Integer points,
            Integer ttlSeconds,
            String eventId,
            String idempotencyKey,
            BigDecimal bookingAmount) {
    }

    private record CommitRequest(
            Long bookingId,
            String holdCode,
            String eventId,
            String idempotencyKey) {
    }

    private record ReleaseRequest(
            Long bookingId,
            String holdCode,
            String reason,
            String eventId,
            String idempotencyKey) {
    }

    private record RefundRequest(
            Long userId,
            Long bookingId,
            Integer pointsToRefund,
            String originalRedeemEventId,
            String eventId,
            String idempotencyKey,
            String reason) {
    }

    private record ScoreHoldPayload(
            String holdCode,
            int pointsHeld,
            String status,
            boolean idempotent,
            BigDecimal discountAmount,
            BigDecimal valuePerPoint) {
    }

    private record ScoreHoldEnvelope(
            boolean success,
            String message,
            String errorCode,
            ScoreHoldPayload data) {
    }
}
