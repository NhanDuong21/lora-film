package com.project.promotionservice.controller;

import com.project.promotionservice.common.ApiResponse;
import com.project.promotionservice.dto.*;
import com.project.promotionservice.service.PromotionAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotions")
public class PromotionAdminController {

    private final PromotionAdminService promotionService;

    public PromotionAdminController(PromotionAdminService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'PROMOTION_CREATE')")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        PromotionResponse response = promotionService.createPromotion(request);
        ApiResponse<PromotionResponse> apiResponse = ApiResponse.success("Promotion created successfully", response);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'PROMOTION_READ')")
    public ResponseEntity<ApiResponse<PromotionPageResponse>> getPromotions(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String availabilityStatus,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 50 || pageable.getPageNumber() < 0) {
            throw new com.project.promotionservice.exception.BusinessException(
                    "Page size must be between 1 and 50, and page index must not be less than zero",
                    "PROMOTION_INVALID_QUERY",
                    HttpStatus.BAD_REQUEST);
        }
        validateSort(pageable.getSort());
        PromotionPageResponse response = promotionService.getPromotions(isActive, availabilityStatus, discountType, campaignId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Promotions retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'PROMOTION_READ')")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(@PathVariable Long id) {
        PromotionResponse response = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.success("Promotion retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'PROMOTION_UPDATE')")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(@PathVariable Long id, @Valid @RequestBody UpdatePromotionRequest request) {
        PromotionResponse response = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'PROMOTION_UPDATE')")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotionStatus(@PathVariable Long id, @Valid @RequestBody UpdatePromotionStatusRequest request) {
        PromotionResponse response = promotionService.updatePromotionStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Promotion status updated successfully", response));
    }

    private void validateSort(org.springframework.data.domain.Sort sort) {
        if (sort == null) return;
        java.util.Set<String> allowedFields = java.util.Set.of("id", "promotionCode", "discountType", "discountValue", "usageLimit", "usedCount", "startDate", "endDate", "createdAt", "updatedAt");
        for (org.springframework.data.domain.Sort.Order order : sort) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new com.project.promotionservice.exception.BusinessException(
                        "Invalid sort property: " + order.getProperty(),
                        "PROMOTION_INVALID_SORT",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }
}
