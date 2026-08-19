package com.project.scoreservice.repository;

import com.project.scoreservice.entity.ScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
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

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.actualPointChange), 0) FROM ScoreHistory s WHERE s.userScore.userId = :userId")
    Integer sumActualPointChangeByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(s.accumulatedAfter - s.accumulatedBefore), 0) FROM ScoreHistory s WHERE s.userScore.userId = :userId")
    Integer sumAccumulatedDeltaByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(CASE WHEN s.actualPointChange > 0 THEN s.actualPointChange ELSE 0 END), 0) FROM ScoreHistory s WHERE s.transactionType IN :types")
    Long sumPositivePointChangeByTransactionTypes(
            @org.springframework.data.repository.query.Param("types") List<ScoreTransactionType> types);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(CASE WHEN s.actualPointChange < 0 THEN -s.actualPointChange ELSE 0 END), 0) FROM ScoreHistory s WHERE s.transactionType IN :types")
    Long sumAbsoluteNegativePointChangeByTransactionTypes(
            @org.springframework.data.repository.query.Param("types") List<ScoreTransactionType> types);
}

