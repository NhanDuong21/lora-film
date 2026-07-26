package com.project.promotionservice.integration.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionOutboxEventRepository extends JpaRepository<PromotionOutboxEvent, Long> {

    Optional<PromotionOutboxEvent> findByPublicId(String publicId);

    @Query("SELECT e FROM PromotionOutboxEvent e " +
           "WHERE e.publishStatus = :status " +
           "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)")
    List<PromotionOutboxEvent> findPendingEvents(@Param("status") OutboxStatus status,
                                                 @Param("now") Instant now,
                                                 Pageable pageable);
}
