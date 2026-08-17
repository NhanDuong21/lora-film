package com.project.notificationservice.repository;

import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.entity.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findByNotificationRequestIdOrderByCreatedAtAsc(Long requestId);

    Optional<NotificationDelivery> findByPublicId(String publicId);

    long countByStatus(DeliveryStatus status);

    @Query("""
            select count(delivery)
            from NotificationDelivery delivery, NotificationRequest request
            where delivery.notificationRequestId = request.id
              and delivery.status = :status
              and delivery.createdAt >= :since
              and (:includeTest = true or request.test = false)
            """)
    long countOperationalByStatus(
            @Param("status") DeliveryStatus status,
            @Param("since") Instant since,
            @Param("includeTest") boolean includeTest);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select delivery from NotificationDelivery delivery
            where delivery.status in :statuses
              and (delivery.nextRetryAt is null or delivery.nextRetryAt <= :now)
            order by delivery.createdAt
            """)
    List<NotificationDelivery> findDue(
            @Param("statuses") Collection<DeliveryStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
