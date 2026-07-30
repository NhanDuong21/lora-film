package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDeliveryAttemptRepository extends JpaRepository<NotificationDeliveryAttempt, Long> {

    List<NotificationDeliveryAttempt> findByNotificationDeliveryIdOrderByAttemptNumberAsc(Long deliveryId);
}
