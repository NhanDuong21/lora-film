package com.lorafilm.booking.service;

import com.lorafilm.booking.reservation.service.impl.RedisLockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RedisLockServiceTest {

    @Mock
    private RedisOperations<String, String> redisTemplate;

    private RedisLockServiceImpl redisLockService;

    @BeforeEach
    public void setUp() {
        redisLockService = new RedisLockServiceImpl(redisTemplate);
    }

    @Test
    public void acquireHoldLocks_Success_ReturnsTrue() {
        Long showtimeId = 100L;
        List<Long> seatIds = List.of(15L, 16L);
        List<String> keys = List.of("seat-lock:100:15", "seat-lock:100:16");
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(keys), eq("token-123"), eq("300"))).thenReturn(1L);

        boolean result = redisLockService.acquireHoldLocks(showtimeId, seatIds, "token-123", 300L);

        assertTrue(result);
    }

    @Test
    public void acquireHoldLocks_Collision_ReturnsFalse() {
        Long showtimeId = 100L;
        List<Long> seatIds = List.of(15L);
        List<String> keys = List.of("seat-lock:100:15");
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(keys), eq("token-123"), eq("300"))).thenReturn(0L);

        boolean result = redisLockService.acquireHoldLocks(showtimeId, seatIds, "token-123", 300L);

        assertFalse(result);
    }
}
