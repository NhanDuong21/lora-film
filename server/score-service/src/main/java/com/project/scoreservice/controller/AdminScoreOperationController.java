package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ReconciliationDetailStatus;
import com.project.scoreservice.enumtype.ReconciliationRunStatus;
import com.project.scoreservice.service.AdminScoreOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/scores")
@Tag(name = "Admin Score General Operations", description = "Endpoints for reconciliation, audit logs, data exports, and dashboard metrics")
public class AdminScoreOperationController {

    private final AdminScoreOperationService adminScoreOperationService;

    public AdminScoreOperationController(AdminScoreOperationService adminScoreOperationService) {
        this.adminScoreOperationService = adminScoreOperationService;
    }

    @PostMapping({"/adjustment", "/adjustments"})
    @Operation(summary = "Adjust user points", description = "Manually add or deduct points for a user with userId in body.")
    public ResponseEntity<ApiResponse<AdminAdjustmentResponse>> adjustScore(
            @Valid @RequestBody ScoreAdjustmentRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Client-Ip", required = false, defaultValue = "127.0.0.1") String clientIp) {
        AdminAdjustmentResponse response = adminScoreOperationService.adjustScore(request.userId(), request, operatorId, clientIp);
        return ResponseEntity.status(Boolean.TRUE.equals(response.getIdempotent()) ? 200 : 201)
                .body(ApiResponse.success("Score adjusted successfully", response));
    }

    @PostMapping({"/reconciliation", "/reconciliations"})
    @Operation(summary = "Trigger reconciliation job", description = "Run balance and ledger consistency check.")
    public ResponseEntity<ApiResponse<ReconciliationDTOs.ReconciliationRunResponse>> runReconciliation(
            @Valid @RequestBody(required = false) ReconciliationDTOs.ReconciliationRunRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId) {
        ReconciliationDTOs.ReconciliationRunResponse response = adminScoreOperationService.runReconciliation(request != null ? request : new ReconciliationDTOs.ReconciliationRunRequest(null, null), operatorId);
        return ResponseEntity.ok(ApiResponse.success("Reconciliation job completed successfully", response));
    }

    @GetMapping({"/reconciliation/runs", "/reconciliations/runs"})
    @Operation(summary = "Get reconciliation runs", description = "List reconciliation batches with pagination and filtering.")
    public ResponseEntity<ApiResponse<PageResponse<ReconciliationDTOs.ReconciliationRunResponse>>> getReconciliationRuns(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) ReconciliationRunStatus status,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        PageResponse<ReconciliationDTOs.ReconciliationRunResponse> response = adminScoreOperationService.getReconciliationRuns(page, size, status, from, to);
        return ResponseEntity.ok(ApiResponse.success("Reconciliation runs retrieved successfully", response));
    }

    @GetMapping({"/reconciliation/details", "/reconciliations/details"})
    @Operation(summary = "Get reconciliation details", description = "List reconciliation details/discrepancies for a batch.")
    public ResponseEntity<ApiResponse<PageResponse<ReconciliationDTOs.ReconciliationDetailResponse>>> getReconciliationDetails(
            @RequestParam(value = "runId", required = false) Long runId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) ReconciliationDetailStatus status) {
        PageResponse<ReconciliationDTOs.ReconciliationDetailResponse> response = adminScoreOperationService.getReconciliationDetails(runId, page, size, status);
        return ResponseEntity.ok(ApiResponse.success("Reconciliation details retrieved successfully", response));
    }

    @GetMapping({"/audit", "/audits", "/audit-logs"})
    @Operation(summary = "Get audit logs", description = "List admin audit logs with multi-field filtering.")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTOs.AuditLogResponse>>> getAuditLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "operatorId", required = false) Long operatorId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        PageResponse<AuditLogDTOs.AuditLogResponse> response = adminScoreOperationService.getAuditLogs(page, size, userId, operatorId, action, from, to);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", response));
    }

    @GetMapping("/export")
    @Operation(summary = "Export score data", description = "Export score history, audit logs, or reconciliation data as CSV.")
    public ResponseEntity<byte[]> exportData(
            @RequestParam(value = "type", defaultValue = "HISTORY") String type,
            @RequestParam(value = "format", defaultValue = "CSV") String format,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        byte[] data = adminScoreOperationService.exportScoreData(type, format, userId, from, to);
        String filename = type.toLowerCase() + "_export_" + LocalDateTime.now().toString().replace(":", "").substring(0, 15) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(data);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get score dashboard stats", description = "Retrieve summary metrics for loyalty admin dashboard.")
    public ResponseEntity<ApiResponse<ScoreDashboardResponse>> getDashboardStats() {
        ScoreDashboardResponse response = adminScoreOperationService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", response));
    }
}
