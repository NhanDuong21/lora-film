package com.project.scoreservice.repository;

import com.project.scoreservice.entity.OutboxEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public class OutboxEventRepositoryImpl implements OutboxEventRepositoryCustom {

    private static final String CLAIM_PENDING_SQL = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING'
              AND (
                  next_retry_at IS NULL
                  OR next_retry_at <= :now
              )
            ORDER BY created_at, id
            FOR UPDATE SKIP LOCKED
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    @SuppressWarnings("unchecked")
    public List<OutboxEvent> findAndClaimPendingEvents(LocalDateTime now, int batchSize) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        Query query = entityManager.createNativeQuery(CLAIM_PENDING_SQL, OutboxEvent.class);
        query.setParameter("now", now);
        query.setMaxResults(batchSize);

        return query.getResultList();
    }
}
