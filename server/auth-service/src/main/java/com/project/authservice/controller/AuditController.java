package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.AuditLogDto;
import com.project.authservice.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/audits")
public class AuditController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditController.class);

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> getAuditLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean attentionOnly,
            Pageable pageable) {
        log.info("Get audit logs called");
        return ResponseEntity.ok(ApiResponse.success("Success",
                auditLogService.getAuditLogs(keyword, attentionOnly, pageable)));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<AuditLogDto>> review(
            @PathVariable Long id,
            @Valid @RequestBody com.project.authservice.dto.request.AuditReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái rà soát",
                auditLogService.review(id, request)));
    }
    public AuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}
