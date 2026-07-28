package com.project.userservice.repository;

import com.project.userservice.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {
    @Query("""
            SELECT log FROM UserAuditLog log
            WHERE (:keyword IS NULL
                OR LOWER(log.action) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(log.targetType) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(log.targetId, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:targetType IS NULL OR log.targetType = :targetType)
            """)
    Page<UserAuditLog> search(@Param("keyword") String keyword,
                              @Param("targetType") String targetType,
                              Pageable pageable);
}
