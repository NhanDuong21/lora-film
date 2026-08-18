package com.project.authservice.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {
    void log(Long accountId, String action, HttpServletRequest request);
    void log(Long accountId, String action, HttpServletRequest request,
             String resourceId, String description);
    
    org.springframework.data.domain.Page<com.project.authservice.dto.AuditLogDto> getAuditLogs(
            String keyword, boolean attentionOnly, org.springframework.data.domain.Pageable pageable);

    com.project.authservice.dto.AuditLogDto review(
            Long id, com.project.authservice.dto.request.AuditReviewRequest request);
}
