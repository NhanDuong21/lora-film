package com.project.scoreservice.controller;
 
import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.enumtype.ScoreTransactionType;
import com.project.scoreservice.exception.BusinessException;
import com.project.scoreservice.service.ScoreService;
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
@RequestMapping("/api/scores/me")
@Tag(name = "Customer Score Balance", description = "Endpoints for logged-in customer's score balance and history")
public class ScoreController {
 
    private final ScoreService scoreService;
 
    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }
 
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("User is not authenticated", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid user ID in authentication token", "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
        }
    }
 
    @GetMapping
    @Operation(summary = "Get current user score balance", description = "Retrieve current available points, accumulated points, and current/next membership tier")
    public ResponseEntity<ApiResponse<UserScoreResponse>> getScoreBalance() {
        Long userId = getCurrentUserId();
        UserScoreResponse response = scoreService.getUserScore(userId);
        return ResponseEntity.ok(ApiResponse.success("Score balance retrieved successfully", response));
    }
  
    @GetMapping("/history")
    @Operation(summary = "Get score history", description = "Retrieve paginated history of point transactions for the authenticated customer")
    public ResponseEntity<ApiResponse<PageResponse<ScoreHistoryResponse>>> getScoreHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "transactionType", required = false) ScoreTransactionType transactionType,
            @RequestParam(value = "bookingId", required = false) Long bookingId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {
 
        Long userId = getCurrentUserId();
        Page<ScoreHistoryResponse> springPage = scoreService.getUserHistory(userId, page, size, transactionType, bookingId, from, to, sort);
        PageResponse<ScoreHistoryResponse> response = new PageResponse<>(springPage);
        return ResponseEntity.ok(ApiResponse.success("Score history retrieved successfully", response));
    }
 
    @PostMapping("/redeem-preview")
    @Operation(summary = "Preview score redemption value", description = "Calculate discount value for booking before actual redemption (does not mutate balance)")
    public ResponseEntity<ApiResponse<RedeemPreviewResponse>> previewRedeem(@Valid @RequestBody RedeemPreviewRequest request) {
        Long userId = getCurrentUserId();
        RedeemPreviewResponse response = scoreService.previewRedeem(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Score redemption preview calculated successfully", response));
    }
}
