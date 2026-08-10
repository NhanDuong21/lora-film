package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationDeadLetterRepository extends JpaRepository<NotificationDeadLetter, Long> {

    Optional<NotificationDeadLetter> findByNotificationDeliveryId(Long deliveryId);
}
