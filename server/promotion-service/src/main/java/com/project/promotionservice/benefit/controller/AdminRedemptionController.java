package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.benefit.dto.response.BenefitResponses.RedemptionResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionType;
import com.project.promotionservice.benefit.service.RedemptionService;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/redemptions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Redemption History", description = "Immutable coupon and voucher redemption history")
public class AdminRedemptionController {

    private final RedemptionService redemptionService;

    public AdminRedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @GetMapping
    @Operation(summary = "Search redemption history")
    public ResponseEntity<ApiResponse<PagedResponse<RedemptionResponse>>> history(
            @RequestParam(required = false) RedemptionType type,
            @RequestParam(required = false) String userPublicId,
            @RequestParam(required = false) String bookingPublicId,
            @RequestParam(required = false) RedemptionStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(redemptionService.history(
                type, userPublicId, bookingPublicId, status, from, to, page, size)));
    }
}
