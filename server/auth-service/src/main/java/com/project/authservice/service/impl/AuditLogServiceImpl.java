package com.project.authservice.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.project.authservice.repository.AuditLogRepository;
import com.project.authservice.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogWriter auditLogWriter;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, AuditLogWriter auditLogWriter) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public void log(Long accountId, String action, HttpServletRequest request) {
        String ipAddress = request == null ? null : clientIp(request);
        String userAgent = request == null ? null : truncate(request.getHeader("User-Agent"), 500);
        String resource = determineResource(action);

        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    writeSafely(accountId, action, resource, ipAddress, userAgent);
                }
            });
            return;
        }
        writeSafely(accountId, action, resource, ipAddress, userAgent);
    }

    private void writeSafely(Long accountId, String action, String resource,
                             String ipAddress, String userAgent) {
        try {
            auditLogWriter.write(accountId, action, resource, ipAddress, userAgent);
            log.info("Audit log written: accountId={}, action={}", accountId, action);
        } catch (RuntimeException exception) {
            log.error("Failed to write audit log for accountId={}, action={}: {}",
                    accountId, action, exception.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isBlank() && !"unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = ipAddress.split(",", 2)[0].trim();
        } else {
            ipAddress = request.getRemoteAddr();
        }
        return truncate(ipAddress, 45);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
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
    public org.springframework.data.domain.Page<com.project.authservice.dto.AuditLogDto> getAuditLogs(
            String keyword, org.springframework.data.domain.Pageable pageable) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Long accountId = null;
        if (normalized != null) {
            try {
                accountId = Long.valueOf(normalized);
            } catch (NumberFormatException ignored) {
                // Text search only.
            }
        }
        org.springframework.data.domain.Pageable safe = org.springframework.data.domain.PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.search(normalized, accountId, safe).map(logEntry ->
                new com.project.authservice.dto.AuditLogDto(
                        logEntry.getId(),
                        logEntry.getAccount() == null ? null : logEntry.getAccount().getId(),
                        logEntry.getAction(),
                        logEntry.getIpAddress(),
                        logEntry.getUserAgent(),
                        logEntry.getCreatedAt()));
    }
}
