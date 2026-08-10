package com.project.promotionservice.integration.job;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SchedulerLockRepository extends JpaRepository<SchedulerLock, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SchedulerLock l where l.jobName = :jobName")
    Optional<SchedulerLock> findForUpdate(@Param("jobName") String jobName);
}
