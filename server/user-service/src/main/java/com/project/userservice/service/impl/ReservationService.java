package com.project.userservice.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class ReservationService {

    private final StringRedisTemplate redisTemplate;

    public ReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to reserve a phone number and CCCD atomically.
     * Returns true if both were successfully reserved, false if either is already reserved.
     * If one succeeds but the other fails, it rolls back the successful one.
     */
    public boolean reserve(String phoneNumber, String cccd, Duration ttl) {
        String phoneKey = "reserved_phone:" + phoneNumber;
        String cccdKey = "reserved_cccd:" + cccd;

        Boolean phoneReserved = redisTemplate.opsForValue().setIfAbsent(phoneKey, "reserved", ttl);
        if (Boolean.TRUE.equals(phoneReserved)) {
            Boolean cccdReserved = redisTemplate.opsForValue().setIfAbsent(cccdKey, "reserved", ttl);
            if (Boolean.TRUE.equals(cccdReserved)) {
                return true;
            } else {
                // Rollback phone reservation
                redisTemplate.delete(phoneKey);
                return false;
            }
        }
        return false;
    }

    public void release(String phoneNumber, String cccd) {
        if (phoneNumber != null) {
            redisTemplate.delete("reserved_phone:" + phoneNumber);
        }
        if (cccd != null) {
            redisTemplate.delete("reserved_cccd:" + cccd);
        }
    }
}
