package com.project.scoreservice.repository;

import com.project.scoreservice.entity.ReconciliationRun;
import com.project.scoreservice.enumtype.ReconciliationRunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
    Optional<ReconciliationRun> findByBatchCode(String batchCode);

    Optional<ReconciliationRun> findTopByOrderByStartedAtDesc();

    Page<ReconciliationRun> findByStatus(ReconciliationRunStatus status, Pageable pageable);

    Page<ReconciliationRun> findByStartedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
}
