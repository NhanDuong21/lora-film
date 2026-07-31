package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationScheduledJob;
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

public interface NotificationScheduledJobRepository
        extends JpaRepository<NotificationScheduledJob, Long> {

    Optional<NotificationScheduledJob> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select job from NotificationScheduledJob job
            where job.status in :statuses
              and job.nextRunAt is not null
              and job.nextRunAt <= :now
              and (job.lockUntil is null or job.lockUntil <= :now)
            order by job.nextRunAt
            """)
    List<NotificationScheduledJob> findDue(
            @Param("statuses") Collection<String> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
