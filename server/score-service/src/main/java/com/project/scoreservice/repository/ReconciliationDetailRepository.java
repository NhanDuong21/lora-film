package com.project.scoreservice.repository;

import com.project.scoreservice.entity.ReconciliationDetail;
import com.project.scoreservice.enumtype.ReconciliationDetailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationDetailRepository extends JpaRepository<ReconciliationDetail, Long> {
    List<ReconciliationDetail> findByRunId(Long runId);

    Page<ReconciliationDetail> findByRunId(Long runId, Pageable pageable);

    Page<ReconciliationDetail> findByRunIdAndStatus(Long runId, ReconciliationDetailStatus status, Pageable pageable);

    List<ReconciliationDetail> findByUserIdAndStatus(Long userId, ReconciliationDetailStatus status);

    long countByRunIdAndStatus(Long runId, ReconciliationDetailStatus status);
}
