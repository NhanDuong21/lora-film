package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {

    Optional<NotificationRecipient> findFirstByNotificationRequestId(Long notificationRequestId);
}
