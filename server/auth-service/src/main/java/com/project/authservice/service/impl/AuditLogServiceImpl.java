package com.project.authservice.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.project.authservice.entity.Account;
import com.project.authservice.entity.AuditLog;
import com.project.authservice.repository.AccountRepository;
import com.project.authservice.repository.AuditLogRepository;
import com.project.authservice.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, AccountRepository accountRepository) {
        this.auditLogRepository = auditLogRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long accountId, String action, HttpServletRequest request) {
        try {
            String ipAddress = null;
            String userAgent = null;
            if (request != null) {
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                userAgent = request.getHeader("User-Agent");
            }

            Account account = null;
            if (accountId != null) {
                account = accountRepository.findById(accountId).orElse(null);
            }

            String resource = determineResource(action);

            AuditLog auditLog = AuditLog.builder()
                    .account(account)
                    .action(action)
                    .resource(resource)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("AuditLog written: accountId={}, action={}, ip={}, userAgent={}",
                    accountId, action, ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Failed to write audit log for accountId={}, action={}: {}", accountId, action, e.getMessage());
        }
    }

    private String determineResource(String action) {
        if (action == null) return "UNKNOWN";
        if (action.contains("ROLE")) return "ROLE";
        if (action.contains("PERMISSION")) return "PERMISSION";
        if (action.contains("ACCOUNT") || action.contains("REGISTER") || action.contains("LOGIN") || action.contains("LOGOUT") || action.contains("PASSWORD") || action.contains("TOKEN")) return "ACCOUNT";
        return "SYSTEM";
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AuditLog> getAuditLogs(org.springframework.data.domain.Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
