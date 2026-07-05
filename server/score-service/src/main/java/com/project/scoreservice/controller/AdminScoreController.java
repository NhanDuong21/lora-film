package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.service.AdminScoreQueryService;
import com.project.scoreservice.service.AdminTierRecalculationService;
import com.project.scoreservice.service.ScoreAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/scores/users")
@Tag(name = "Admin Score Operations", description = "Endpoints for administrator's score management and auditing")
public class AdminScoreController {

    private final AdminScoreQueryService adminScoreQueryService;
    private final ScoreAdjustmentService scoreAdjustmentService;
    private final AdminTierRecalculationService adminTierRecalculationService;

    public AdminScoreController(AdminScoreQueryService adminScoreQueryService,
                                ScoreAdjustmentService scoreAdjustmentService,
                                AdminTierRecalculationService adminTierRecalculationService) {
        this.adminScoreQueryService = adminScoreQueryService;
        this.scoreAdjustmentService = scoreAdjustmentService;
        this.adminTierRecalculationService = adminTierRecalculationService;
    }

    private Long getCurrentAdminId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("User is not authenticated", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid admin ID in authentication token", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
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
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        Page<AdminScoreHistoryItemResponse> springPage = adminScoreQueryService.getUserHistory(
                userId, page, size, transactionType, bookingId, reconciliationStatus, from, to, sort
        );
        PageResponse<AdminScoreHistoryItemResponse> response = new PageResponse<>(springPage);
        return ResponseEntity.ok(ApiResponse.success("Score history retrieved successfully", response));
    }

    @PostMapping("/{userId}/adjustments")
    @Operation(summary = "Perform manual score adjustment", description = "Add or deduct points manually for a user's score account.")
    public ResponseEntity<ApiResponse<ScoreAdjustmentResponse>> adjustScore(
            @PathVariable Long userId,
            @Valid @RequestBody ScoreAdjustmentRequest request) {

        Long adminId = getCurrentAdminId();
        ScoreAdjustmentResponse response = scoreAdjustmentService.adjustScore(userId, request, adminId);
        HttpStatus status = Boolean.TRUE.equals(response.getIdempotent()) ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity<>(ApiResponse.success("User score adjusted successfully", response), status);
    }

    @PostMapping("/{userId}/recalculate-tier")
    @Operation(summary = "Recalculate user tier", description = "Explicitly recalculate and update a user's membership tier based on accumulated points.")
    public ResponseEntity<ApiResponse<RecalculateTierResponse>> recalculateTier(@PathVariable Long userId) {
        RecalculateTierResponse response = adminTierRecalculationService.recalculateUserTier(userId);
        return ResponseEntity.ok(ApiResponse.success("Membership tier recalculated successfully", response));
    }
}
