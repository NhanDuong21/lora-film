package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ReconciliationStatus;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.service.AdminScoreQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    public AdminScoreController(AdminScoreQueryService adminScoreQueryService) {
        this.adminScoreQueryService = adminScoreQueryService;
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
}
