package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.service.AdminScoreQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/scores/users")
@Tag(name = "Admin Score Operations", description = "Endpoints for administrator's score management and auditing")
public class AdminScoreController {

    private final AdminScoreQueryService adminScoreQueryService;
    private final com.project.scoreservice.service.ScoreService scoreService;
    private final com.project.scoreservice.service.AdminScoreOperationService adminScoreOperationService;

    public AdminScoreController(AdminScoreQueryService adminScoreQueryService,
                                com.project.scoreservice.service.ScoreService scoreService,
                                com.project.scoreservice.service.AdminScoreOperationService adminScoreOperationService) {
        this.adminScoreQueryService = adminScoreQueryService;
        this.scoreService = scoreService;
        this.adminScoreOperationService = adminScoreOperationService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user score detail", description = "Retrieve score detail of a user. Returns 404 if the score account does not exist.")
    public ResponseEntity<ApiResponse<AdminUserScoreResponse>> getUserScoreDetail(@PathVariable Long userId) {
        AdminUserScoreResponse response = adminScoreQueryService.getUserScoreDetail(userId);
        return ResponseEntity.ok(ApiResponse.success("User score retrieved successfully", response));
    }

    @GetMapping("/{userId}/history")
    @Operation(summary = "Get user score history", description = "Retrieve score history of a user with filtering, pagination, and sorting.")
    public ResponseEntity<ApiResponse<PageResponse<AdminScoreHistoryItemResponse>>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "transactionType", required = false) ScoreTransactionType transactionType,
            @RequestParam(value = "bookingId", required = false) Long bookingId,
            @RequestParam(value = "reconciliationStatus", required = false) ReconciliationStatus reconciliationStatus,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "sort", defaultValue = "occurredAt,desc") String sort) {

        Page<AdminScoreHistoryItemResponse> springPage = adminScoreQueryService.getUserHistory(
                userId, page, size, transactionType, bookingId, reconciliationStatus, from, to, sort
        );
        PageResponse<AdminScoreHistoryItemResponse> response = new PageResponse<>(springPage);
        return ResponseEntity.ok(ApiResponse.success("Score history retrieved successfully", response));
    }

    @GetMapping("/{userId}/expiring")
    @Operation(summary = "Get user expiring points", description = "Retrieve expiring point buckets of a user for admin auditing.")
    public ResponseEntity<ApiResponse<java.util.List<ExpiringPointResponse>>> getUserExpiringPoints(@PathVariable Long userId) {
        java.util.List<ExpiringPointResponse> response = scoreService.getExpiringPoints(userId);
        return ResponseEntity.ok(ApiResponse.success("Expiring points retrieved successfully", response));
    }

    @GetMapping("/{userId}/tier-history")
    @Operation(summary = "Get user tier history", description = "Retrieve membership tier history of a user for admin auditing.")
    public ResponseEntity<ApiResponse<java.util.List<TierHistoryItemResponse>>> getUserTierHistory(@PathVariable Long userId) {
        java.util.List<TierHistoryItemResponse> response = scoreService.getTierHistory(userId);
        return ResponseEntity.ok(ApiResponse.success("Tier history retrieved successfully", response));
    }

    @PostMapping("/{userId}/adjustments")
    @Operation(summary = "Adjust user points", description = "Manually add or deduct points for a user.")
    public ResponseEntity<ApiResponse<AdminAdjustmentResponse>> adjustUserScore(
            @PathVariable Long userId,
            @RequestBody ScoreAdjustmentRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Client-Ip", required = false, defaultValue = "127.0.0.1") String clientIp) {
        AdminAdjustmentResponse response = adminScoreOperationService.adjustScore(userId, request, operatorId, clientIp);
        return ResponseEntity.status(Boolean.TRUE.equals(response.getIdempotent()) ? 200 : 201)
                .body(ApiResponse.success("Score adjusted successfully", response));
    }

    @PostMapping("/{userId}/adjustments/reverse")
    @Operation(summary = "Reverse an adjustment", description = "Reverse a previous adjustment transaction.")
    public ResponseEntity<ApiResponse<AdminAdjustmentResponse>> reverseUserAdjustment(
            @PathVariable Long userId,
            @RequestBody ReverseAdjustmentRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Client-Ip", required = false, defaultValue = "127.0.0.1") String clientIp) {
        AdminAdjustmentResponse response = adminScoreOperationService.reverseAdjustment(userId, request, operatorId, clientIp);
        return ResponseEntity.status(201).body(ApiResponse.success("Adjustment reversed successfully", response));
    }

    @PostMapping("/{userId}/recalculate-tier")
    @Operation(summary = "Recalculate user tier", description = "Recalculate membership tier for a user.")
    public ResponseEntity<ApiResponse<AdminAdjustmentResponse>> recalculateTier(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Client-Ip", required = false, defaultValue = "127.0.0.1") String clientIp) {
        AdminAdjustmentResponse response = adminScoreOperationService.recalculateTier(userId, operatorId, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Tier recalculated successfully", response));
    }

    @PostMapping("/{userId}/status")
    @Operation(summary = "Freeze or unfreeze a score account", description = "Changes loyalty operations only; it does not block customer login.")
    public ResponseEntity<ApiResponse<AdminUserScoreResponse>> updateScoreAccountStatus(
            @PathVariable Long userId,
            @Valid @RequestBody ScoreAccountStatusRequest request,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
            @RequestHeader(value = "X-Client-Ip", required = false, defaultValue = "127.0.0.1") String clientIp) {
        AdminUserScoreResponse response = adminScoreOperationService.updateAccountStatus(userId, request, operatorId, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Score account status updated successfully", response));
    }
}

