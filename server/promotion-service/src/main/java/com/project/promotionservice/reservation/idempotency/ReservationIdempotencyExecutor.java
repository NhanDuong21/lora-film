package com.project.promotionservice.reservation.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class ReservationIdempotencyExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationIdempotencyExecutor.class);
    private static final int MAX_KEY_LENGTH = 255;

    private final ReservationIdempotencyStore store;
    private final ObjectMapper objectMapper;

    public ReservationIdempotencyExecutor(
            ReservationIdempotencyStore store,
            ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public ReservationResponse execute(
            String clientId,
            String apiName,
            String idempotencyKey,
            String reservationPublicId,
            Object requestPayload,
            int successStatus,
            Supplier<ReservationResponse> action) {
        String key = requireKey(idempotencyKey);
        String normalizedClient = requireClient(clientId);
        String requestHash = requestHash(reservationPublicId, requestPayload);
        ReservationIdempotencyStore.Claim claim = claim(
                normalizedClient, apiName, key, requestHash);
        if (!claim.acquired()) {
            return replay(claim.responseBody());
        }

        ReservationResponse response;
        try {
            response = action.get();
        } catch (RuntimeException | Error throwable) {
            try {
                store.fail(normalizedClient, apiName, key);
            } catch (RuntimeException failureUpdateException) {
                log.error("Unable to mark idempotency record as failed", failureUpdateException);
            }
            throw throwable;
        }

        try {
            store.complete(
                    normalizedClient,
                    apiName,
                    key,
                    objectMapper.writeValueAsString(response),
                    successStatus,
                    response == null ? reservationPublicId : response.getPublicId());
        } catch (RuntimeException | JsonProcessingException completionException) {
            // The domain transaction has already committed. Returning its response is safer
            // than reporting a false business failure; a stale PROCESSING lease is replay-safe.
            log.error("Domain command committed but idempotency completion failed", completionException);
        }
        return response;
    }

    private ReservationIdempotencyStore.Claim claim(
            String clientId, String apiName, String key, String requestHash) {
        try {
            return store.claim(clientId, apiName, key, requestHash, apiName, "POST");
        } catch (DataIntegrityViolationException race) {
            try {
                return store.claim(clientId, apiName, key, requestHash, apiName, "POST");
            } catch (DataIntegrityViolationException repeatedRace) {
                throw new BusinessException(
                        ReservationErrorCode.RESERVATION_CONFLICT,
                        "Concurrent idempotency claim could not be resolved",
                        HttpStatus.CONFLICT);
            }
        }
    }

    private ReservationResponse replay(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Completed idempotency record has no replayable response",
                    HttpStatus.CONFLICT);
        }
        try {
            JsonNode stored = objectMapper.readTree(responseBody);
            while (stored != null && stored.isTextual()) {
                stored = objectMapper.readTree(stored.asText());
            }
            return objectMapper.treeToValue(stored, ReservationResponse.class);
        } catch (JsonProcessingException exception) {
            log.error("Unable to deserialize stored reservation idempotency response", exception);
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CONFLICT,
                    "Stored idempotency response is invalid",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String requestHash(String reservationPublicId, Object requestPayload) {
        ObjectNode root = objectMapper.createObjectNode();
        if (reservationPublicId != null) {
            root.put("reservationPublicId", reservationPublicId);
        }
        root.set("payload", canonicalize(objectMapper.valueToTree(requestPayload)));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(root));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to hash idempotency payload", exception);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isNull()) {
            return objectMapper.nullNode();
        }
        if (node.isObject()) {
            ObjectNode sorted = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            java.util.List<Map.Entry<String, JsonNode>> entries = new java.util.ArrayList<>();
            fields.forEachRemaining(entries::add);
            entries.sort(Comparator.comparing(Map.Entry::getKey));
            entries.forEach(entry ->
                    sorted.set(entry.getKey(), canonicalize(entry.getValue())));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = objectMapper.createArrayNode();
            node.forEach(value -> array.add(canonicalize(value)));
            return array;
        }
        return node;
    }

    private String requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "X-Idempotency-Key is required and must not exceed 255 characters",
                    HttpStatus.BAD_REQUEST);
        }
        return key.trim();
    }

    private String requireClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_IDEMPOTENCY_CONFLICT,
                    "Authenticated internal service identity is required",
                    HttpStatus.UNAUTHORIZED);
        }
        return clientId.trim();
    }
}
