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
}
