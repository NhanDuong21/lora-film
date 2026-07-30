package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.showtime.domain.entity.ShowtimeRefundOutboxEvent;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeRefundOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShowtimeRefundOutboxRepository
        extends JpaRepository<ShowtimeRefundOutboxEvent, Long> {
    Optional<ShowtimeRefundOutboxEvent> findByEventId(String eventId);

    @Query("""
            select e from ShowtimeRefundOutboxEvent e
            where e.status in :statuses
              and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)
              and (e.lockedUntil is null or e.lockedUntil <= :now)
            order by e.id
            """)
    List<ShowtimeRefundOutboxEvent> findReady(
            @Param("statuses") Collection<ShowtimeRefundOutboxStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ShowtimeRefundOutboxEvent e where e.id = :id")
    Optional<ShowtimeRefundOutboxEvent> findByIdForUpdate(@Param("id") Long id);
}
