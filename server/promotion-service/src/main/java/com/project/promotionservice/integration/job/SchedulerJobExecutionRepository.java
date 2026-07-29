package com.project.promotionservice.integration.job;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SchedulerJobExecutionRepository extends JpaRepository<SchedulerJobExecution, Long> {
    List<SchedulerJobExecution> findByJobNameOrderByStartedAtDesc(String jobName, Pageable pageable);

    @Modifying
    @Query("delete from SchedulerJobExecution e where e.startedAt < :cutoff")
    int deleteStartedBefore(@Param("cutoff") Instant cutoff);
}
