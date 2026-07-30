package com.project.notificationservice.repository;

import com.project.notificationservice.entity.NotificationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationAuditLogRepository extends JpaRepository<NotificationAuditLog, Long> {
}
