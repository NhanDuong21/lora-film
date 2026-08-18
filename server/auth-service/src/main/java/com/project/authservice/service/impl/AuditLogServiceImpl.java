package com.project.authservice.service.impl;

import com.project.authservice.dto.AuditLogDto;
import com.project.authservice.dto.request.AuditReviewRequest;
import com.project.authservice.entity.AuditLog;
import com.project.authservice.exception.BusinessException;
import com.project.authservice.exception.ResourceNotFoundException;
import com.project.authservice.repository.AuditLogRepository;
import com.project.authservice.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final Set<String> SENSITIVE_ACTIONS = Set.of(
            "UPDATE_ACCOUNT_ROLE", "UPDATE_ACCOUNT_ACCESS_PROFILE",
            "UPDATE_MANAGER_CINEMA_ASSIGNMENTS", "UPDATE_ROLE", "DELETE_ROLE",
            "CREATE_PERMISSION", "UPDATE_PERMISSION", "DELETE_PERMISSION",
            "ADMIN_REVOKED_ALL_SESSIONS");

    private final AuditLogRepository auditLogRepository;
    private final AuditLogWriter auditLogWriter;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, AuditLogWriter auditLogWriter) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public void log(Long accountId, String action, HttpServletRequest request) {
        log(accountId, action, request, accountId == null ? null : accountId.toString(), null);
    }

    @Override
    public void log(Long accountId, String action, HttpServletRequest request,
                    String resourceId, String description) {
        String ipAddress = request == null ? null : clientIp(request);
        String userAgent = request == null ? null : truncate(request.getHeader("User-Agent"), 500);
        String resource = determineResource(action);
        String result = determineResult(action);
        String severity = determineSeverity(action);
        String reviewStatus = "NORMAL".equals(severity) ? "NOT_REQUIRED" : "UNREVIEWED";
        Long actorAccountId = currentActorAccountId();

        Runnable write = () -> writeSafely(accountId, actorAccountId, action, resource,
                resourceId, description, result, severity, reviewStatus, ipAddress, userAgent);

        // Failure logs must survive a business rollback; the independent writer does that.
        if ("FAILED".equals(result)) {
            write.run();
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        write.run();
                    }
                }
            });
            return;
        }
        write.run();
    }

    private void writeSafely(Long accountId, Long actorAccountId, String action, String resource,
                             String resourceId, String description, String result, String severity,
                             String reviewStatus, String ipAddress, String userAgent) {
        try {
            auditLogWriter.write(accountId, actorAccountId, action, resource, resourceId,
                    description, result, severity, reviewStatus, ipAddress, userAgent);
            log.info("Audit log written: accountId={}, actorAccountId={}, action={}",
                    accountId, actorAccountId, action);
        } catch (RuntimeException exception) {
            log.error("Failed to write audit log for accountId={}, action={}: {}",
                    accountId, action, exception.getMessage());
        }
    }

    private Long currentActorAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object credentials = authentication.getCredentials();
        if (credentials instanceof Number number) return number.longValue();
        if (credentials instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String determineResource(String action) {
        if (action == null) return "UNKNOWN";
        if (action.contains("ROLE") || action.contains("ACCESS_PROFILE")) return "ACCESS_CONTROL";
        if (action.contains("PERMISSION")) return "PERMISSION";
        if (action.contains("ACCOUNT") || action.contains("REGISTER") || action.contains("LOGIN")
                || action.contains("LOGOUT") || action.contains("PASSWORD") || action.contains("TOKEN")
                || action.contains("INVITATION") || action.contains("SESSION")) return "ACCOUNT";
        return "SYSTEM";
    }

    private String determineResult(String action) {
        if (action == null) return "SUCCESS";
        String normalized = action.toUpperCase(Locale.ROOT);
        return normalized.contains("FAILED") || normalized.contains("REJECTED")
                || normalized.contains("BLOCKED") || normalized.contains("ERROR")
                ? "FAILED" : "SUCCESS";
    }

    private String determineSeverity(String action) {
        if (action == null) return "NORMAL";
        if ("LOGIN_FAILED_INVALID_PASSWORD".equals(action) || SENSITIVE_ACTIONS.contains(action)) {
            return "REVIEW";
        }
        return "NORMAL";
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(String keyword, boolean attentionOnly, Pageable pageable) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Long accountId = null;
        if (normalized != null) {
            try {
                accountId = Long.valueOf(normalized);
            } catch (NumberFormatException ignored) {
                // Text search only.
            }
        }
        Pageable safe = PageRequest.of(Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.search(normalized, accountId, attentionOnly, safe).map(this::toDto);
    }

    @Override
    @Transactional
    public AuditLogDto review(Long id, AuditReviewRequest request) {
        AuditLog entry = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi hoạt động"));
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("REVIEWED", "RESOLVED").contains(status)) {
            throw new BusinessException("Trạng thái rà soát không hợp lệ");
        }
        entry.setReviewStatus(status);
        entry.setReviewedBy(currentActorAccountId());
        entry.setReviewNote(request.note() == null || request.note().isBlank()
                ? null : request.note().trim());
        entry.setReviewedAt(LocalDateTime.now());
        return toDto(auditLogRepository.save(entry));
    }

    private AuditLogDto toDto(AuditLog entry) {
        return new AuditLogDto(entry.getId(),
                entry.getAccount() == null ? null : entry.getAccount().getId(),
                entry.getCreatedBy(), entry.getAction(), entry.getResource(), entry.getResourceId(),
                entry.getDescription(), entry.getResult(), entry.getSeverity(), entry.getReviewStatus(),
                entry.getReviewedBy(), entry.getReviewNote(), entry.getReviewedAt(),
                entry.getIpAddress(), entry.getUserAgent(), entry.getCreatedAt());
    }
}
