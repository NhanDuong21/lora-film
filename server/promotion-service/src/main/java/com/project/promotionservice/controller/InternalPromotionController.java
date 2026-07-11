package com.project.promotionservice.controller;

import com.project.promotionservice.common.ApiResponse;
import com.project.promotionservice.dto.ApplyPromotionRequest;
import com.project.promotionservice.dto.ApplyPromotionResponse;
import com.project.promotionservice.dto.ConfirmUsageRequest;
import com.project.promotionservice.dto.PromotionUsageResponse;
import com.project.promotionservice.dto.RevertUsageRequest;
import com.project.promotionservice.service.PromotionApplyService;
import com.project.promotionservice.service.PromotionUsageService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/promotions")
public class InternalPromotionController {

    private final PromotionApplyService promotionApplyService;
    private final PromotionUsageService promotionUsageService;

    public InternalPromotionController(PromotionApplyService promotionApplyService,
                                       PromotionUsageService promotionUsageService) {
        this.promotionApplyService = promotionApplyService;
        this.promotionUsageService = promotionUsageService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyPromotionResponse>> applyPromotion(
            @RequestHeader(value = "X-Internal-Token") @Parameter(hidden = true) String internalToken,
            @Valid @RequestBody ApplyPromotionRequest request) {
        
        ApplyPromotionResponse response = promotionApplyService.applyPromotion(request);
        
        if (Boolean.TRUE.equals(response.getIdempotent())) {
            return ResponseEntity.ok(
                    ApiResponse.success("Promotion was already applied to this booking", response)
            );
        } else {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success("Promotion applied successfully", response)
            );
        }
    }

    @PostMapping("/usages/{usageId}/confirm")
    public ResponseEntity<ApiResponse<PromotionUsageResponse>> confirmUsage(
            @RequestHeader(value = "X-Internal-Token") @Parameter(hidden = true) String internalToken,
            @PathVariable Long usageId,
            @Valid @RequestBody ConfirmUsageRequest request) {
        
        PromotionUsageResponse response = promotionUsageService.confirmUsage(usageId, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion usage confirmed successfully", response));
    }

    @PostMapping("/usages/{usageId}/revert")
    public ResponseEntity<ApiResponse<PromotionUsageResponse>> revertUsage(
            @RequestHeader(value = "X-Internal-Token") @Parameter(hidden = true) String internalToken,
            @PathVariable Long usageId,
            @Valid @RequestBody RevertUsageRequest request) {
        
        PromotionUsageResponse response = promotionUsageService.revertUsage(usageId, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion usage reverted successfully", response));
    }

    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<ApiResponse<PromotionUsageResponse>> getUsageByBookingId(
            @RequestHeader(value = "X-Internal-Token") @Parameter(hidden = true) String internalToken,
            @PathVariable Long bookingId) {
        
        PromotionUsageResponse response = promotionUsageService.getUsageByBookingId(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Promotion usage retrieved successfully", response));
    }
}


