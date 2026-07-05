package com.project.userservice.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import com.project.userservice.dto.ReservationResult;

@Service
public class ReservationService {

    private final StringRedisTemplate redisTemplate;

    public ReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to reserve a phone number and CCCD atomically.
     * Returns a ReservationResult containing success status, errorCode, and retryAfterSeconds.
     */
    public ReservationResult reserve(String phoneNumber, String cccd, Duration ttl) {
        String phoneKey = "reserved_phone:" + phoneNumber;
        String cccdKey = "reserved_cccd:" + cccd;

        boolean phoneHasKey = Boolean.TRUE.equals(redisTemplate.hasKey(phoneKey));
        boolean cccdHasKey = Boolean.TRUE.equals(redisTemplate.hasKey(cccdKey));

        if (phoneHasKey && cccdHasKey) {
            Long expire = redisTemplate.getExpire(phoneKey, TimeUnit.SECONDS);
            return new ReservationResult(false, "PHONE_NUMBER_AND_CCCD_RESERVED", expire != null && expire > 0 ? expire : null);
        } else if (phoneHasKey) {
            Long expire = redisTemplate.getExpire(phoneKey, TimeUnit.SECONDS);
            return new ReservationResult(false, "PHONE_NUMBER_RESERVED", expire != null && expire > 0 ? expire : null);
        } else if (cccdHasKey) {
            Long expire = redisTemplate.getExpire(cccdKey, TimeUnit.SECONDS);
            return new ReservationResult(false, "CCCD_RESERVED", expire != null && expire > 0 ? expire : null);
        }

        Boolean phoneReserved = redisTemplate.opsForValue().setIfAbsent(phoneKey, "reserved", ttl);
        if (Boolean.TRUE.equals(phoneReserved)) {
            Boolean cccdReserved = redisTemplate.opsForValue().setIfAbsent(cccdKey, "reserved", ttl);
            if (Boolean.TRUE.equals(cccdReserved)) {
                return new ReservationResult(true, null, null);
            } else {
                // Rollback phone reservation
                redisTemplate.delete(phoneKey);
                Long expire = redisTemplate.getExpire(cccdKey, TimeUnit.SECONDS);
                return new ReservationResult(false, "CCCD_RESERVED", expire != null && expire > 0 ? expire : null);
            }
        }
        
        Long expire = redisTemplate.getExpire(phoneKey, TimeUnit.SECONDS);
        return new ReservationResult(false, "PHONE_NUMBER_RESERVED", expire != null && expire > 0 ? expire : null);
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
