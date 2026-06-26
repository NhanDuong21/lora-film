package com.project.scoreservice.repository;

import com.project.scoreservice.entity.ScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, Long>, JpaSpecificationExecutor<ScoreHistory> {

    Optional<ScoreHistory> findByIdempotencyKey(String idempotencyKey);

    Optional<ScoreHistory> findByEventId(String eventId);

    Optional<ScoreHistory> findByRequestId(String requestId);
}
