package com.project.promotionservice.promotion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.reservation.idempotency.ReservationIdempotencyStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmergencyCommandIdempotencyExecutorTest {

    @Test
    void completedKeyReplaysResponseWithoutExecutingReleaseAgain() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ReservationIdempotencyStore store = mock(ReservationIdempotencyStore.class);
        EmergencyCommandIdempotencyExecutor executor =
                new EmergencyCommandIdempotencyExecutor(store, objectMapper);
        ForceReleaseImpactResponse stored = new ForceReleaseImpactResponse(
                "campaign-1", 2, 2, new BigDecimal("10000.00"),
                new BigDecimal("10000.00"), 0, 2, 4, "impact-token",
                Instant.parse("2026-08-20T00:00:00Z"), 2, 0, 0,
                true, List.of());
        when(store.claim(eq("promotion-admin:admin"),
                eq("ADMIN_CAMPAIGN_FORCE_RELEASE"), eq("command-1"),
                anyString(), anyString(), eq("POST")))
                .thenReturn(ReservationIdempotencyStore.Claim.replay(
                        objectMapper.writeValueAsString(stored), 200));
        AtomicInteger executions = new AtomicInteger();

        ForceReleaseImpactResponse result = executor.execute(
                "admin", "command-1", "campaign-1", "payload", () -> {
                    executions.incrementAndGet();
                    return stored;
                });

        assertThat(result).isEqualTo(stored);
        assertThat(executions).hasValue(0);
        verify(store, never()).complete(anyString(), anyString(), anyString(),
                anyString(), eq(200), anyString());
    }
}
