package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

    Optional<NotificationLog> findByEventId(String eventId);

    Optional<NotificationLog> findByIdempotencyKey(String idempotencyKey);
}
