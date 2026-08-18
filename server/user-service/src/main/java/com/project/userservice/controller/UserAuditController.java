package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.UserAuditResponse;
import com.project.userservice.service.UserAuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user-audits")
public class UserAuditController {
    private final UserAuditService auditService;

    public UserAuditController(UserAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')"
            + " or hasAuthority('USER_AUDIT_VIEW')")
    public ApiResponse<Page<UserAuditResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "false") boolean attentionOnly,
            Pageable pageable) {
        return ApiResponse.success("Audit logs retrieved",
                auditService.search(keyword, targetType, attentionOnly, pageable));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ApiResponse<UserAuditResponse> review(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody
            com.project.userservice.dto.request.UserAuditReviewRequest request) {
        return ApiResponse.success("Đã cập nhật trạng thái rà soát",
                auditService.review(id, request));
    }
}
