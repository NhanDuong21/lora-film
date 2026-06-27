package com.project.bookingservice.service.lock;

import com.project.bookingservice.config.BookingProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Service
public class SeatLockManager {

    private final StringRedisTemplate redisTemplate;
    private final BookingProperties bookingProperties;

    private static final String LOCK_PREFIX = "booking:seat-lock:";

    public SeatLockManager(StringRedisTemplate redisTemplate,
                           BookingProperties bookingProperties) {
        this.redisTemplate = redisTemplate;
        this.bookingProperties = bookingProperties;
    }

    public boolean acquireLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        List<String> keys = seatIds.stream()
                .map(seatId -> getLockKey(showtimeId, seatId))
                .collect(Collectors.toList());

        long ttl = bookingProperties.getRedis().getLock().getTtlSeconds();

        String script = 
            "for i, key in ipairs(KEYS) do " +
            "  if redis.call('EXISTS', key) == 1 then " +
            "    return 0 " +
            "  end " +
            "end " +
            "for i, key in ipairs(KEYS) do " +
            "  redis.call('SET', key, ARGV[1], 'EX', ARGV[2]) " +
            "end " +
            "return 1 ";

        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(redisScript, keys, lockOwner, String.valueOf(ttl));

        return result != null && result == 1L;
    }

    public void releaseLocks(Long showtimeId, List<Long> seatIds, String lockOwner) {
        List<String> keys = seatIds.stream()
                .map(seatId -> getLockKey(showtimeId, seatId))
                .collect(Collectors.toList());

        if (keys.isEmpty()) {
            return;
        }

        String script = 
            "local count = 0 " +
            "for i, key in ipairs(KEYS) do " +
            "  if redis.call('GET', key) == ARGV[1] then " +
            "    redis.call('DEL', key) " +
            "    count = count + 1 " +
            "  end " +
            "end " +
            "return count ";

        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        redisTemplate.execute(redisScript, keys, lockOwner);
    }

    public void forceReleaseLocks(Long showtimeId, List<Long> seatIds) {
        List<String> keys = seatIds.stream()
                .map(seatId -> getLockKey(showtimeId, seatId))
                .collect(Collectors.toList());

        if (keys.isEmpty()) {
            return;
        }

        redisTemplate.delete(keys);
    }

    private String getLockKey(Long showtimeId, Long seatId) {
        return LOCK_PREFIX + "{" + showtimeId + "}:" + seatId;
    }
}
