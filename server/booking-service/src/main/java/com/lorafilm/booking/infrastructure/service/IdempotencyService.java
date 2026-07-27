package com.lorafilm.booking.infrastructure.service;

import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;

import java.util.Optional;

public interface IdempotencyService {

    Optional<BookingIdempotencyKey> checkKey(String idempotencyKey);

    /**
     * Looks up a key in its customer/action scope. The legacy overload is
     * retained for older callers and tests, but runtime compatibility routes
     * must use this scoped variant.
     */
    default Optional<BookingIdempotencyKey> checkKey(
            String idempotencyKey, Long userId, String endpoint) {
        return checkKey(idempotencyKey)
                .filter(record -> java.util.Objects.equals(record.getUserId(), userId)
                        && java.util.Objects.equals(record.getEndpoint(), endpoint));
    }

    BookingIdempotencyKey startProcessing(String idempotencyKey, Long userId, String endpoint, String httpMethod, Object requestBody);

    void completeProcessing(String idempotencyKey, int responseStatus, Object responseBody);

    void failProcessing(String idempotencyKey);

    default void completeProcessing(
            String idempotencyKey, Long userId, String endpoint,
            int responseStatus, Object responseBody) {
        completeProcessing(idempotencyKey, responseStatus, responseBody);
    }

    default void failProcessing(String idempotencyKey, Long userId, String endpoint) {
        failProcessing(idempotencyKey);
    }
}
