package com.project.userservice.controller;

import com.project.userservice.dto.request.PiiRetentionRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.PiiGovernanceSummaryResponse;
import com.project.userservice.service.PiiGovernanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/pii-governance")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "PII governance")
public class PiiGovernanceController {
    private final PiiGovernanceService service;

    public PiiGovernanceController(PiiGovernanceService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PiiGovernanceSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("PII governance summary retrieved", service.summary()));
    }

    @PostMapping("/users/{accountId}/retention")
    public ResponseEntity<ApiResponse<Void>> scheduleRetention(
            @PathVariable Long accountId, @Valid @RequestBody PiiRetentionRequest request) {
        service.scheduleRetention(accountId, request);
        return ResponseEntity.ok(ApiResponse.success("PII retention scheduled", null));
    }

    @PostMapping("/erase-due")
    public ResponseEntity<ApiResponse<Integer>> eraseDue() {
        return ResponseEntity.ok(ApiResponse.success("Expired PII erased", service.anonymizeExpiredRecords()));
    }
}
