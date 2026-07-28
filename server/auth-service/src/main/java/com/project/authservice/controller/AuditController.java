package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.AuditLogDto;
import com.project.authservice.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/audits")
public class AuditController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditController.class);

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) String keyword, Pageable pageable) {
        log.info("Get audit logs called");
        return ResponseEntity.ok(ApiResponse.success("Success", auditLogService.getAuditLogs(keyword, pageable)));
    }
    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}
