package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetter, Long> {

    Optional<NotificationDeadLetter> findByNotificationDeliveryId(Long deliveryId);

    Page<NotificationDeadLetter> findAllByReprocessedAtIsNull(Pageable pageable);

    long countByReprocessedAtIsNull();
}
