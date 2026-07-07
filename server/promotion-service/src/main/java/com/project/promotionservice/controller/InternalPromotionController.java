package com.project.promotionservice.controller;

import com.project.promotionservice.common.ApiResponse;
import com.project.promotionservice.dto.ApplyPromotionRequest;
import com.project.promotionservice.dto.ApplyPromotionResponse;
import com.project.promotionservice.service.PromotionApplyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/promotions")
public class InternalPromotionController {

    private final PromotionApplyService promotionApplyService;

    public InternalPromotionController(PromotionApplyService promotionApplyService) {
        this.promotionApplyService = promotionApplyService;
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyPromotionResponse>> applyPromotion(
            @RequestHeader(value = "X-Internal-Token") String internalToken,
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
}
