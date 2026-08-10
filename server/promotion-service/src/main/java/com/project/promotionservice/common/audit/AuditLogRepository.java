package com.project.promotionservice.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByPublicId(String publicId);

    List<AuditLog> findByEntityTypeAndEntityPublicId(String entityType, String entityPublicId);
}
