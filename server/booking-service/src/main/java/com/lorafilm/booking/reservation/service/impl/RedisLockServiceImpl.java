package com.lorafilm.booking.reservation.service.impl;

import com.lorafilm.booking.reservation.service.RedisLockService;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RedisLockServiceImpl implements RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockServiceImpl.class);

    private final RedisOperations<String, String> redisTemplate;
    private final DefaultRedisScript<Long> holdScript;
    private final DefaultRedisScript<Long> releaseScript;
    private final BookingMetricsManager bookingMetricsManager;

    public RedisLockServiceImpl(RedisOperations<String, String> redisTemplate, BookingMetricsManager bookingMetricsManager) {
        this.redisTemplate = redisTemplate;
        this.bookingMetricsManager = bookingMetricsManager;

        this.holdScript = new DefaultRedisScript<>();
        this.holdScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/hold_seats.lua")));
        this.holdScript.setResultType(Long.class);

        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/release_seats.lua")));
        this.releaseScript.setResultType(Long.class);
    }

    @Override
    public boolean acquireHoldLocks(Long showtimeId, List<Long> seatIds, String lockOwner, long ttlSeconds) {
        List<String> keys = buildSeatLockKeys(showtimeId, seatIds);
        Long result = redisTemplate.execute(holdScript, keys, lockOwner, String.valueOf(ttlSeconds));
        boolean success = result != null && result == 1L;
        if (success) {
            bookingMetricsManager.incrementRedisLockSuccess();
        } else {
            bookingMetricsManager.incrementRedisLockFailed();
            log.warn("Failed to acquire Redis locks for keys: {}", keys);
        }
        return success;
    }

    @Override
    public void releaseLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        List<String> keys = buildSeatLockKeys(showtimeId, seatIds);
        redisTemplate.execute(releaseScript, keys, lockOwner);
    }

    @Override
    public boolean acquireSingleLock(String lockKey, String lockOwner, long ttlSeconds) {
        Long result = redisTemplate.execute(holdScript, List.of(lockKey), lockOwner, String.valueOf(ttlSeconds));
        boolean success = result != null && result == 1L;
        if (success) {
            bookingMetricsManager.incrementRedisLockSuccess();
        } else {
            bookingMetricsManager.incrementRedisLockFailed();
        }
        return success;
    }

    @Override
    public void releaseSingleLock(String lockKey, String lockOwner) {
        redisTemplate.execute(releaseScript, List.of(lockKey), lockOwner);
    }

    @Override
    public boolean extendLockTtl(Long showtimeId, Long seatId, String lockOwner, long newTtlSeconds) {
        String key = "seat-lock:" + showtimeId + ":" + seatId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            return Boolean.TRUE.equals(redisTemplate.expire(key, java.time.Duration.ofSeconds(newTtlSeconds)));
        } else {
            return acquireSingleLock(key, lockOwner, newTtlSeconds);
        }
    }

    private List<String> buildSeatLockKeys(Long showtimeId, List<Long> seatIds) {
        List<String> keys = new ArrayList<>(seatIds.size());
        for (Long seatId : seatIds) {
            keys.add("seat-lock:" + showtimeId + ":" + seatId);
        }
        return keys;
    }
}
