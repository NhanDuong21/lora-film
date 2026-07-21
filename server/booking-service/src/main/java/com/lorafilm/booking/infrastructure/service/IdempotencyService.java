package com.lorafilm.booking.infrastructure.service;

import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;

import java.util.Optional;

public interface IdempotencyService {

    Optional<BookingIdempotencyKey> checkKey(String idempotencyKey);

    BookingIdempotencyKey startProcessing(String idempotencyKey, Long userId, String endpoint, String httpMethod, Object requestBody);

    void completeProcessing(String idempotencyKey, int responseStatus, Object responseBody);

    void failProcessing(String idempotencyKey);
}
