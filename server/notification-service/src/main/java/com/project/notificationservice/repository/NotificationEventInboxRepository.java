package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationEventInbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationEventInboxRepository extends JpaRepository<NotificationEventInbox, Long> {

    boolean existsBySourceServiceAndSourceEventId(String sourceService, String sourceEventId);

    Optional<NotificationEventInbox> findBySourceServiceAndSourceEventId(
            String sourceService, String sourceEventId);
}
