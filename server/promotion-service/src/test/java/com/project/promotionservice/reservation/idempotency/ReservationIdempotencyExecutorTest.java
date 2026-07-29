package com.project.promotionservice.reservation.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.reservation.dto.response.ReservationResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationIdempotencyExecutorTest {

    private final ReservationIdempotencyStore store =
            mock(ReservationIdempotencyStore.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ReservationIdempotencyExecutor executor =
            new ReservationIdempotencyExecutor(store, objectMapper);

    @Test
    void completedClaimReplaysResponseWithoutRunningDomainCommand() throws Exception {
        ReservationResponse stored = new ReservationResponse();
        stored.setPublicId("reservation-1");
        when(store.claim(
                eq("BOOKING_SERVICE"),
                eq("POST /internal/reservations"),
                eq("stable-key"),
                anyString(),
                anyString(),
                eq("POST")))
                .thenReturn(ReservationIdempotencyStore.Claim.replay(
                        objectMapper.writeValueAsString(stored), 201));
        AtomicBoolean invoked = new AtomicBoolean();

        ReservationResponse replay = executor.execute(
                "BOOKING_SERVICE",
                "POST /internal/reservations",
                "stable-key",
                null,
                new Payload("value"),
                201,
                () -> {
                    invoked.set(true);
                    return new ReservationResponse();
                });

        assertThat(replay.getPublicId()).isEqualTo("reservation-1");
        assertThat(invoked).isFalse();
        verify(store, never()).complete(
                anyString(), anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void successfulCommandPersistsReplayableResponse() {
        when(store.claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ReservationIdempotencyStore.Claim.acquiredClaim());
        ReservationResponse response = new ReservationResponse();
        response.setPublicId("reservation-2");

        ReservationResponse result = executor.execute(
                "PAYMENT_SERVICE",
                "POST /internal/reservations/{reservationId}/confirm",
                "confirm-key",
                "reservation-2",
                new Payload("payment-1"),
                200,
                () -> response);

        assertThat(result).isSameAs(response);
        verify(store).complete(
                eq("PAYMENT_SERVICE"),
                eq("POST /internal/reservations/{reservationId}/confirm"),
                eq("confirm-key"),
                anyString(),
                eq(200),
                eq("reservation-2"));
    }

    @Test
    void failedCommandReleasesTheProcessingLeaseForRetry() {
        when(store.claim(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ReservationIdempotencyStore.Claim.acquiredClaim());

        assertThatThrownBy(() -> executor.execute(
                "BOOKING_SERVICE",
                "POST /internal/reservations/{reservationId}/cancel",
                "cancel-key",
                "reservation-3",
                new Payload("reason"),
                200,
                () -> {
                    throw new IllegalStateException("domain failure");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("domain failure");

        verify(store).fail(
                "BOOKING_SERVICE",
                "POST /internal/reservations/{reservationId}/cancel",
                "cancel-key");
    }

    private record Payload(String value) {
    }
}
