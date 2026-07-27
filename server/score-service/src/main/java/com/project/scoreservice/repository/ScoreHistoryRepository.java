package com.project.scoreservice.repository;

import com.project.scoreservice.entity.ScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project.scoreservice.enumtype.ReconciliationStatus;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, Long>, JpaSpecificationExecutor<ScoreHistory> {

    Optional<ScoreHistory> findByIdempotencyKey(String idempotencyKey);

    Optional<ScoreHistory> findByEventId(String eventId);

    Optional<ScoreHistory> findByRequestId(String requestId);

    List<ScoreHistory> findByBookingId(Long bookingId);

    List<ScoreHistory> findByReconciliationStatusOrderByCreatedAtAsc(ReconciliationStatus reconciliationStatus);

    boolean existsByReferenceHistoryAndTransactionType(ScoreHistory referenceHistory, com.project.scoreservice.enumtype.ScoreTransactionType transactionType);

    List<ScoreHistory> findByReferenceHistoryAndTransactionType(ScoreHistory referenceHistory, com.project.scoreservice.enumtype.ScoreTransactionType transactionType);

    List<ScoreHistory> findByReferenceHistory(ScoreHistory referenceHistory);
}

