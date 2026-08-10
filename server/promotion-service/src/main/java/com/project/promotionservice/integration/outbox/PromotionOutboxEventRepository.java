package com.project.promotionservice.integration.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface PromotionOutboxEventRepository extends JpaRepository<PromotionOutboxEvent, Long> {

    Optional<PromotionOutboxEvent> findByPublicId(String publicId);

    long countByPublishStatus(OutboxStatus status);

    List<PromotionOutboxEvent> findByPublishStatusIn(Collection<OutboxStatus> statuses, Pageable pageable);

    List<PromotionOutboxEvent> findByPublishStatusOrderByCreatedAtAsc(OutboxStatus status, Pageable pageable);

    @Query("SELECT e FROM PromotionOutboxEvent e " +
           "WHERE e.publishStatus = :status " +
           "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now)")
    List<PromotionOutboxEvent> findPendingEvents(@Param("status") OutboxStatus status,
                                                  @Param("now") Instant now,
                                                  Pageable pageable);

    @Query("""
            select e.id from PromotionOutboxEvent e
            where e.deletedAt is null
              and (
                    (
                        e.publishStatus = com.project.promotionservice.integration.outbox.OutboxStatus.PENDING
                        and (e.nextRetryAt is null or e.nextRetryAt <= :now)
                    )
                    or (
                        e.publishStatus = com.project.promotionservice.integration.outbox.OutboxStatus.PROCESSING
                        and (e.processingStartedAt is null or e.processingStartedAt <= :staleBefore)
                    )
              )
            order by e.createdAt asc
            """)
    List<Long> findClaimableIds(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable);

    @Modifying
    @Query("""
            delete from PromotionOutboxEvent e
            where e.publishStatus = :published
              and e.publishedAt is not null
              and e.publishedAt <= :cutoff
            """)
    int deletePublishedBefore(
            @Param("published") OutboxStatus published,
            @Param("cutoff") Instant cutoff);
}
