package com.project.bookingservice.service;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.project.bookingservice.config.BookingProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BookingProperties bookingProperties;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate, BookingProperties bookingProperties) {
        this.redisTemplate = redisTemplate;
        this.bookingProperties = bookingProperties;
    }

    public boolean acquire(String idempotencyKey) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                "idempotency:" + idempotencyKey,
                new IdempotencyRecord(), // Placeholder record with null fields
                bookingProperties.getIdempotency().getTtlHours(),
                TimeUnit.HOURS
        );
        return Boolean.TRUE.equals(acquired);
    }

    public void save(String idempotencyKey, IdempotencyRecord record) {
        redisTemplate.opsForValue().set(
                "idempotency:" + idempotencyKey,
                record,
                bookingProperties.getIdempotency().getTtlHours(),
                TimeUnit.HOURS
        );
    }

    public IdempotencyRecord get(String idempotencyKey) {
        Object record = redisTemplate.opsForValue().get("idempotency:" + idempotencyKey);
        if (record instanceof IdempotencyRecord) {
            return (IdempotencyRecord) record;
        }
        return null;
    }

    public void remove(String idempotencyKey) {
        redisTemplate.delete("idempotency:" + idempotencyKey);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    public static class IdempotencyRecord {
        private String requestHash;
        private int responseStatus;
        private byte[] responseBody;
        private String contentType;

        public IdempotencyRecord() {}

        public IdempotencyRecord(String requestHash, int responseStatus, byte[] responseBody, String contentType) {
            this.requestHash = requestHash;
            this.responseStatus = responseStatus;
            this.responseBody = responseBody;
            this.contentType = contentType;
        }

        public String getRequestHash() {
            return requestHash;
        }

        public void setRequestHash(String requestHash) {
            this.requestHash = requestHash;
        }

        public int getResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(int responseStatus) {
            this.responseStatus = responseStatus;
        }

        public byte[] getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(byte[] responseBody) {
            this.responseBody = responseBody;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }
}
