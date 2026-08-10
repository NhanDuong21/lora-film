package com.project.authservice.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {
    void log(Long accountId, String action, HttpServletRequest request);
    
    org.springframework.data.domain.Page<com.project.authservice.dto.AuditLogDto> getAuditLogs(
            String keyword, org.springframework.data.domain.Pageable pageable);
}
