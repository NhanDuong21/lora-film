package com.project.bookingservice.service.lock;

import com.project.bookingservice.config.BookingProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SeatLockManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BookingProperties bookingProperties;

    private static final String LOCK_PREFIX = "booking:seat-lock:";

    public SeatLockManager(RedisTemplate<String, Object> redisTemplate,
                           BookingProperties bookingProperties) {
        this.redisTemplate = redisTemplate;
        this.bookingProperties = bookingProperties;
    }

    public boolean acquireLocks(Long showtimeId, List<Long> seatIds, String reservationId) {
        Map<String, Object> locks = seatIds.stream()
                .collect(Collectors.toMap(
                        seatId -> getLockKey(showtimeId, seatId),
                        seatId -> reservationId
                ));

        // MSETNX: Atomically sets multiple keys if none of them already exist.
        Boolean success = redisTemplate.opsForValue().multiSetIfAbsent(locks);

        if (Boolean.TRUE.equals(success)) {
            // Apply TTL to all keys (MSETNX doesn't support TTL natively)
            long ttl = bookingProperties.getRedis().getLock().getTtlSeconds();
            
            // To ensure best performance we could use pipelining, but simple loop is fine for small number of seats
            for (String key : locks.keySet()) {
                redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
            return true;
        }

        return false;
    }

    public void releaseLocks(Long showtimeId, List<Long> seatIds) {
        List<String> keys = seatIds.stream()
                .map(seatId -> getLockKey(showtimeId, seatId))
                .collect(Collectors.toList());

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String getLockKey(Long showtimeId, Long seatId) {
        return LOCK_PREFIX + showtimeId + ":" + seatId;
    }
}
