package com.project.authservice.repository;

import com.project.authservice.entity.AuditLog;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	List<AuditLog> findByAccountIdOrderByCreatedAtDesc(Long accountId);

	@Query("""
			SELECT log FROM AuditLog log
			LEFT JOIN log.account account
			WHERE (:keyword IS NULL
				OR LOWER(log.action) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(log.resource) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(COALESCE(log.ipAddress, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(COALESCE(log.userAgent, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR LOWER(COALESCE(account.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
				OR (:accountId IS NOT NULL AND account.id = :accountId))
			  AND (:attentionOnly = FALSE OR (log.severity IN ('REVIEW', 'CRITICAL')
			       AND log.reviewStatus IN ('UNREVIEWED', 'IN_PROGRESS')))
			""")
	Page<AuditLog> search(@Param("keyword") String keyword,
			@Param("accountId") Long accountId,
			@Param("attentionOnly") boolean attentionOnly,
			Pageable pageable);

	List<AuditLog> findByAccountIdAndActionAndCreatedAtAfterOrderByCreatedAtAsc(
			Long accountId, String action, LocalDateTime createdAt);

	@Query("""
			SELECT COUNT(DISTINCT log.account.id) FROM AuditLog log
			WHERE log.action = 'LOGIN_FAILED_INVALID_PASSWORD'
			  AND log.ipAddress = :ipAddress
			  AND log.account IS NOT NULL
			  AND log.createdAt >= :createdAt
			""")
	long countDistinctTargetAccountsFromSourceSince(
			@Param("ipAddress") String ipAddress,
			@Param("createdAt") LocalDateTime createdAt);

	boolean existsByActionAndIpAddressAndSeverityAndCreatedAtAfter(
			String action, String ipAddress, String severity, LocalDateTime createdAt);

	boolean existsByAccountIdAndActionAndSeverityAndCreatedAtAfter(
			Long accountId, String action, String severity, LocalDateTime createdAt);
}
