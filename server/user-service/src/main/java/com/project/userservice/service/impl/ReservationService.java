package com.project.userservice.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.project.userservice.dto.ReservationResult;

@Service
public class ReservationService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local phoneOwner = redis.call('GET', KEYS[1])
            local cccdOwner = redis.call('GET', KEYS[2])
            local phoneConflict = phoneOwner and phoneOwner ~= ARGV[1]
            local cccdConflict = cccdOwner and cccdOwner ~= ARGV[1]
            if phoneConflict and cccdConflict then return 3 end
            if phoneConflict then return 1 end
            if cccdConflict then return 2 end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('SET', KEYS[2], ARGV[1], 'PX', ARGV[2])
            return 0
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
            local deleted = 0
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              deleted = deleted + redis.call('DEL', KEYS[1])
            end
            if redis.call('GET', KEYS[2]) == ARGV[1] then
              deleted = deleted + redis.call('DEL', KEYS[2])
            end
            return deleted
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public ReservationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to reserve a phone number and CCCD atomically.
     * Returns a ReservationResult containing success status, errorCode, and retryAfterSeconds.
     */
    public ReservationResult reserve(String phoneNumber, String cccd, String ownerRequestId, Duration ttl) {
        if (ownerRequestId == null || ownerRequestId.isBlank()) {
            throw new IllegalArgumentException("Reservation owner request ID is required");
        }
        String phoneKey = "reserved_phone:" + phoneNumber;
        String cccdKey = "reserved_cccd:" + cccd;
        Long result = redisTemplate.execute(RESERVE_SCRIPT, List.of(phoneKey, cccdKey),
                ownerRequestId, String.valueOf(ttl.toMillis()));
        int status = result == null ? -1 : result.intValue();
        return switch (status) {
            case 0 -> new ReservationResult(true, null, null);
            case 1 -> conflict("PHONE_NUMBER_RESERVED", phoneKey);
            case 2 -> conflict("CCCD_RESERVED", cccdKey);
            case 3 -> new ReservationResult(false, "PHONE_NUMBER_AND_CCCD_RESERVED",
                    remainingTtl(phoneKey));
            default -> throw new IllegalStateException("Redis reservation script returned no result");
        };
    }

    public void release(String phoneNumber, String cccd, String ownerRequestId) {
        if (phoneNumber == null || cccd == null || ownerRequestId == null || ownerRequestId.isBlank()) {
            return;
        }
        redisTemplate.execute(RELEASE_SCRIPT,
                List.of("reserved_phone:" + phoneNumber, "reserved_cccd:" + cccd),
                ownerRequestId);
    }

    private ReservationResult conflict(String errorCode, String key) {
        return new ReservationResult(false, errorCode, remainingTtl(key));
    }

    private Long remainingTtl(String key) {
        Long seconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return seconds != null && seconds > 0 ? seconds : null;
    }
}
