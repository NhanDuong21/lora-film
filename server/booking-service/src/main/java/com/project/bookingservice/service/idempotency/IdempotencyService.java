package com.project.bookingservice.service.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.dto.reservation.ReservationResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.project.bookingservice.dto.reservation.CreateReservationRequest;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BookingProperties bookingProperties;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "booking:idempotency:";

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate,
                              BookingProperties bookingProperties,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.bookingProperties = bookingProperties;
        this.objectMapper = objectMapper;
    }

    public void saveResponse(Long userId, String idempotencyKey, Object requestPayload, List<ReservationResponse> response) {
        try {
            String key = getKey(userId, idempotencyKey);
            IdempotencyRecord record = new IdempotencyRecord(
                    serializeCanonicalPayload(requestPayload),
                    response
            );
            long ttlHours = bookingProperties.getIdempotency().getTtlHours();
            redisTemplate.opsForValue().set(key, record, ttlHours, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotency record", e);
        }
    }

    public List<ReservationResponse> getResponse(Long userId, String idempotencyKey, Object requestPayload) {
        String key = getKey(userId, idempotencyKey);
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof IdempotencyRecord record) {
            if ("PROCESSING".equals(record.getRequestHash())) {
                return null;
            }
            try {
                String currentRequestHash = serializeCanonicalPayload(requestPayload);
                if (record.getRequestHash().equals(currentRequestHash)) {
                    return record.getResponse();
                } else {
                    return null; // Signals a conflict
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize request payload for comparison", e);
            }
        }
        return null;
    }

    public boolean tryAcquire(Long userId, String idempotencyKey) {
        String key = getKey(userId, idempotencyKey);
        IdempotencyRecord pendingRecord = new IdempotencyRecord("PROCESSING", null);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, pendingRecord, 5, TimeUnit.MINUTES));
    }

    private String getKey(Long userId, String idempotencyKey) {
        return IDEMPOTENCY_PREFIX + userId + ":" + idempotencyKey;
    }

    private String serializeCanonicalPayload(Object requestPayload) throws JsonProcessingException {
        if (requestPayload instanceof CreateReservationRequest req && req.getSeatIds() != null) {
            Collections.sort(req.getSeatIds());
        }
        return objectMapper.writeValueAsString(requestPayload);
    }

    public static class IdempotencyRecord {
        private String requestHash;
        private List<ReservationResponse> response;

        public IdempotencyRecord() {
        }

        public IdempotencyRecord(String requestHash, List<ReservationResponse> response) {
            this.requestHash = requestHash;
            this.response = response;
        }

        public String getRequestHash() {
            return requestHash;
        }

        public void setRequestHash(String requestHash) {
            this.requestHash = requestHash;
        }

        public List<ReservationResponse> getResponse() {
            return response;
        }

        public void setResponse(List<ReservationResponse> response) {
            this.response = response;
        }
    }
}
