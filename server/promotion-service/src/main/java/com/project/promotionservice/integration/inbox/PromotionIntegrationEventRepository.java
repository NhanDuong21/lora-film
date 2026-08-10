package com.project.promotionservice.integration.inbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionIntegrationEventRepository
        extends JpaRepository<PromotionIntegrationEvent, Long> {
    Optional<PromotionIntegrationEvent> findByPublicId(String publicId);
    Optional<PromotionIntegrationEvent> findBySourceServiceAndEventId(String sourceService, String eventId);
    long countByProcessingStatus(IntegrationEventStatus status);
    List<PromotionIntegrationEvent> findByProcessingStatusIn(Collection<IntegrationEventStatus> status,
                                                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PromotionIntegrationEvent e where e.publicId = :publicId")
    Optional<PromotionIntegrationEvent> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select e from PromotionIntegrationEvent e
            where e.processingStatus = com.project.promotionservice.integration.inbox.IntegrationEventStatus.RETRY
              and (e.nextRetryAt is null or e.nextRetryAt <= :now)
            order by e.createdAt asc
            """)
    List<PromotionIntegrationEvent> findDueRetries(@Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("""
            delete from PromotionIntegrationEvent e
            where e.processingStatus in :statuses and e.createdAt < :cutoff
            """)
    int deleteProcessedBefore(@Param("statuses") Collection<IntegrationEventStatus> statuses,
                              @Param("cutoff") Instant cutoff);
}
