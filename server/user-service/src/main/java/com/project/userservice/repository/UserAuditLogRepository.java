package com.project.userservice.repository;

import com.project.userservice.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
}
