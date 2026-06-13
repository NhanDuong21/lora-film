package com.project.authservice.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditLogService {
    void log(Long accountId, String action, HttpServletRequest request);
}
