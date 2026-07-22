package com.lorafilm.booking.reservation.service;

import java.util.List;

public interface RedisLockService {

    boolean acquireHoldLocks(Long showtimeId, List<Long> seatIds, String lockOwner, long ttlSeconds);

    void releaseLocks(Long showtimeId, List<Long> seatIds, String lockOwner);

    boolean acquireSingleLock(String lockKey, String lockOwner, long ttlSeconds);

    void releaseSingleLock(String lockKey, String lockOwner);

    boolean extendLockTtl(Long showtimeId, Long seatId, String lockOwner, long newTtlSeconds);
}
