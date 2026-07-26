package com.project.authservice.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {
    void log(Long accountId, String action, HttpServletRequest request);
    
    org.springframework.data.domain.Page<com.project.authservice.entity.AuditLog> getAuditLogs(org.springframework.data.domain.Pageable pageable);
}
