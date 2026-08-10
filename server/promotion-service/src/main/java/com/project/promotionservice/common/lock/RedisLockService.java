package com.project.promotionservice.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public LockAttempt tryAcquire(String key, String owner, Duration ttl) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, owner, ttl);
            return Boolean.TRUE.equals(success)
                    ? LockAttempt.ACQUIRED
                    : LockAttempt.CONTENDED;
        } catch (Exception ex) {
            // Redis is an advisory contention gate. Database row locks and
            // unique constraints remain the source of correctness.
            log.warn("Redis lock unavailable for key {}; falling back to database locking", key);
            return LockAttempt.UNAVAILABLE;
        }
    }

    public void release(String key, String owner) {
        try {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(key), owner);
        } catch (Exception ex) {
            log.warn("Unable to release Redis lock for key {}; TTL will release it", key);
        }
    }

    public enum LockAttempt {
        ACQUIRED,
        CONTENDED,
        UNAVAILABLE
    }
}
