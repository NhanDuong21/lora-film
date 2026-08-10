package com.project.paymentservice.repository;

import com.project.paymentservice.entity.PaymentOutboxEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public class PaymentOutboxEventRepositoryImpl implements PaymentOutboxEventRepositoryCustom {

    private static final String CLAIM_PENDING_SQL = """
            SELECT *
            FROM payment_outbox_events
            WHERE (
                    status IN ('PENDING', 'FAILED')
                    AND (next_retry_at IS NULL OR next_retry_at <= :now)
                  )
               OR (
                    status = 'PROCESSING'
                    AND locked_until <= :now
                  )
            ORDER BY created_at, id
            FOR UPDATE SKIP LOCKED
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    @SuppressWarnings("unchecked")
    public List<PaymentOutboxEvent> findAndClaimPendingEvents(
            Instant now, Instant lockedUntil, String ownerToken, int batchSize) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        Query query = entityManager.createNativeQuery(CLAIM_PENDING_SQL, PaymentOutboxEvent.class);
        query.setParameter("now", now);
        query.setMaxResults(batchSize);

        List<PaymentOutboxEvent> events = query.getResultList();
        for (PaymentOutboxEvent event : events) {
            event.setStatus(com.project.paymentservice.enumtype.OutboxStatus.PROCESSING);
            event.setLockedBy(ownerToken);
            event.setLockedAt(now);
            event.setLockedUntil(lockedUntil);
        }
        entityManager.flush();
        return events;
    }
}
