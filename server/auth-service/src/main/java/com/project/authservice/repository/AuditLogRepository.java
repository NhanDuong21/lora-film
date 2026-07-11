package com.project.authservice.repository;

import com.project.authservice.entity.AuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
