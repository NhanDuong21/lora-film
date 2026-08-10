package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
}
