package com.project.promotionservice.controller;

import com.project.promotionservice.common.ApiResponse;
import com.project.promotionservice.dto.PromotionResponse;
import com.project.promotionservice.dto.PromotionValidationRequest;
import com.project.promotionservice.dto.PromotionValidationResponse;
import com.project.promotionservice.exception.BusinessException;
import com.project.promotionservice.service.PromotionService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Set;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> getActivePromotions(
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 50 || pageable.getPageNumber() < 0) {
            throw new BusinessException(
                    "Page size must be between 1 and 50, and page index must not be less than zero",
                    "PROMOTION_INVALID_QUERY",
                    HttpStatus.BAD_REQUEST);
        }
        validateSort(pageable.getSort());
        Page<PromotionResponse> response = promotionService.getActivePromotions(discountType, minOrderAmount, pageable);
        return ResponseEntity.ok(ApiResponse.success("Active promotions retrieved successfully", response));
    }

    @GetMapping("/{promotionId}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionDetail(@PathVariable Long promotionId) {
        PromotionResponse response = promotionService.getPromotionDetail(promotionId);
        return ResponseEntity.ok(ApiResponse.success("Promotion retrieved successfully", response));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionValidationResponse>> validatePromotion(
            @RequestHeader(value = "Authorization", required = false) @Parameter(hidden = true) String authHeader,
            @Valid @RequestBody PromotionValidationRequest request) {
        PromotionValidationResponse response = promotionService.validatePromotion(request, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Promotion is valid", response));
    }

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<PromotionValidationResponse>> previewDiscount(
            @RequestHeader(value = "Authorization", required = false) @Parameter(hidden = true) String authHeader,
            @Valid @RequestBody PromotionValidationRequest request) {
        PromotionValidationResponse response = promotionService.previewDiscount(request, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Discount preview calculated successfully", response));
    }

    private void validateSort(org.springframework.data.domain.Sort sort) {
        if (sort == null) return;
        Set<String> allowedFields = Set.of("id", "promotionCode", "discountType", "discountValue", "usageLimit", "usedCount", "startDate", "endDate", "createdAt", "updatedAt");
        for (org.springframework.data.domain.Sort.Order order : sort) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new BusinessException(
                        "Invalid sort property: " + order.getProperty(),
                        "PROMOTION_INVALID_SORT",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }
}
