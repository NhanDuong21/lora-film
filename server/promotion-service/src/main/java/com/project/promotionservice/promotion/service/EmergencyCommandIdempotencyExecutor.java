package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.reservation.idempotency.ReservationIdempotencyStore;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.Supplier;

/** Durable replay guard for the break-glass force-release command. */
@Service
public class EmergencyCommandIdempotencyExecutor {

    private static final String API_NAME = "ADMIN_CAMPAIGN_FORCE_RELEASE";
    private static final Logger log = LoggerFactory.getLogger(
            EmergencyCommandIdempotencyExecutor.class);

    private final ReservationIdempotencyStore store;
    private final ObjectMapper objectMapper;

    public EmergencyCommandIdempotencyExecutor(
            ReservationIdempotencyStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public ForceReleaseImpactResponse execute(
            String actor, String idempotencyKey, String campaignPublicId,
            Object request, Supplier<ForceReleaseImpactResponse> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 255) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Idempotency-Key is required and must not exceed 255 characters",
                    HttpStatus.BAD_REQUEST);
        }
        String clientId = "promotion-admin:" + (actor == null ? "SYSTEM" : actor);
        String key = idempotencyKey.trim();
        String hash = hash(campaignPublicId, request);
        ReservationIdempotencyStore.Claim claim;
        try {
            claim = store.claim(clientId, API_NAME, key, hash,
                    "/api/admin/promotion-campaigns/" + campaignPublicId
                            + "/force-release", "POST");
        } catch (DataIntegrityViolationException race) {
            claim = store.claim(clientId, API_NAME, key, hash,
                    "/api/admin/promotion-campaigns/" + campaignPublicId
                            + "/force-release", "POST");
        }
        if (!claim.acquired()) {
            return replay(claim.responseBody());
        }
        try {
            ForceReleaseImpactResponse response = action.get();
            try {
                store.complete(clientId, API_NAME, key,
                        objectMapper.writeValueAsString(response), HttpStatus.OK.value(),
                        campaignPublicId);
            } catch (Exception replayPersistenceFailure) {
                // The domain command has already committed. Never report a false command
                // failure merely because persisting the replay response failed.
                log.error("Force-release completed but its replay response could not be persisted "
                        + "for campaign {} and key {}", campaignPublicId, key,
                        replayPersistenceFailure);
            }
            return response;
        } catch (RuntimeException exception) {
            store.fail(clientId, API_NAME, key);
            throw exception;
        }
    }

    private ForceReleaseImpactResponse replay(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, ForceReleaseImpactResponse.class);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Stored force-release response is not replayable",
                    HttpStatus.CONFLICT);
        }
    }

    private String hash(String campaignPublicId, Object request) {
        try {
            byte[] input = objectMapper.writeValueAsString(
                    java.util.Map.of("campaignPublicId", campaignPublicId,
                            "request", request)).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(input));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash force-release request", exception);
        }
    }
}
