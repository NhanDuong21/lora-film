package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.benefit.dto.request.RedemptionRequests.BenefitValidationRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.ValidationResponse;
import com.project.promotionservice.benefit.service.RedemptionService;
import com.project.promotionservice.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@Tag(
        name = "Internal Benefit Validation APIs",
        description = "Read-only coupon and voucher validation before creating a checkout reservation")
@SecurityRequirement(name = "internalTokenAuth")
public class InternalBenefitController {

    private final RedemptionService redemptionService;

    public InternalBenefitController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping("/coupons/validate")
    @Operation(summary = "Validate coupon")
    public ResponseEntity<ApiResponse<ValidationResponse>> validateCoupon(
            @Valid @RequestBody BenefitValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(redemptionService.validateCoupon(request)));
    }

    @PostMapping("/vouchers/validate")
    @Operation(summary = "Validate voucher")
    public ResponseEntity<ApiResponse<ValidationResponse>> validateVoucher(
            @Valid @RequestBody BenefitValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(redemptionService.validateVoucher(request)));
    }
}
