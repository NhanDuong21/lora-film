package com.project.bookingservice.service.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookingservice.config.BookingProperties;
import com.project.bookingservice.dto.reservation.ReservationGroupResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

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

    public void saveResponse(Long userId, String idempotencyKey, Object requestPayload,
            ReservationGroupResponse response) {
        try {
            String key = getKey(userId, idempotencyKey);
            IdempotencyRecord record = new IdempotencyRecord(
                    objectMapper.writeValueAsString(requestPayload),
                    response);
            long ttlHours = bookingProperties.getIdempotency().getTtlHours();
            redisTemplate.opsForValue().set(key, record, ttlHours, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotency record", e);
        }
    }

    public ReservationGroupResponse getResponse(Long userId, String idempotencyKey, Object requestPayload) {
        String key = getKey(userId, idempotencyKey);
        Object value = redisTemplate.opsForValue().get(key);

        if (value instanceof IdempotencyRecord record) {
            try {
                String currentRequestHash = objectMapper.writeValueAsString(requestPayload);
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

    public boolean hasKey(Long userId, String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getKey(userId, idempotencyKey)));
    }

    private String getKey(Long userId, String idempotencyKey) {
        return IDEMPOTENCY_PREFIX + userId + ":" + idempotencyKey;
    }

    public static class IdempotencyRecord {
        private String requestHash;
        private ReservationGroupResponse response;

        public IdempotencyRecord() {
        }

        public IdempotencyRecord(String requestHash, ReservationGroupResponse response) {
            this.requestHash = requestHash;
            this.response = response;
        }

        public String getRequestHash() {
            return requestHash;
        }

        public void setRequestHash(String requestHash) {
            this.requestHash = requestHash;
        }

        public ReservationGroupResponse getResponse() {
            return response;
        }

        public void setResponse(ReservationGroupResponse response) {
            this.response = response;
        }
    }
}
