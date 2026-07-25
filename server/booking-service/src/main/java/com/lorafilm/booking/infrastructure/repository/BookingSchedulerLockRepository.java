package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingSchedulerLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface BookingSchedulerLockRepository extends JpaRepository<BookingSchedulerLock, Long> {

    Optional<BookingSchedulerLock> findBySchedulerName(String schedulerName);

    @Modifying
    @Transactional
    @Query("UPDATE BookingSchedulerLock l " +
           "SET l.status = 'LOCKED', l.lockOwner = :owner, l.lockedAt = :now, l.expiresAt = :expiresAt " +
           "WHERE l.schedulerName = :schedulerName " +
           "AND (l.status = 'RELEASED' OR l.expiresAt <= :now)")
    int tryAcquireLock(@Param("schedulerName") String schedulerName,
                       @Param("owner") String owner,
                       @Param("now") Instant now,
                       @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Transactional
    @Query("UPDATE BookingSchedulerLock l " +
           "SET l.status = 'RELEASED' " +
           "WHERE l.schedulerName = :schedulerName " +
           "AND l.lockOwner = :owner")
    int releaseLock(@Param("schedulerName") String schedulerName,
                    @Param("owner") String owner);
}
