package com.project.authservice.service.impl;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.AuditLog;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.AuditLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit entries in an independent transaction after the audited
 * business transaction has released its database locks.
 */
@Component
public class AuditLogWriter {
    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository, AccountRepository accountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Long accountId, Long actorAccountId, String action, String resource,
                      String resourceId, String description, String result, String severity,
                      String reviewStatus, String ipAddress, String userAgent) {
        Account account = accountId == null
                ? null
                : accountRepository.findById(accountId).orElse(null);
        AuditLog auditLog = AuditLog.builder()
                .account(account)
                .action(action)
                .resource(resource)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLog.setResourceId(resourceId);
        auditLog.setDescription(description);
        auditLog.setResult(result);
        auditLog.setSeverity(severity);
        auditLog.setReviewStatus(reviewStatus);
        auditLog.setCreatedBy(actorAccountId);
        auditLogRepository.saveAndFlush(auditLog);
    }
}
