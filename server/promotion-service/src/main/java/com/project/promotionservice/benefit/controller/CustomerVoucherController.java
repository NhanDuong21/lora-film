package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.service.VoucherService;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/customers/me/vouchers")
@Tag(name = "Customer Voucher Wallet", description = "Authenticated customer voucher wallet")
public class CustomerVoucherController {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "validTo", "status", "name");

    private final VoucherService voucherService;

    public CustomerVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    @Operation(summary = "Get current customer voucher wallet")
    public ResponseEntity<ApiResponse<PagedResponse<VoucherResponse>>> wallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) VoucherStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "validTo,asc") @Size(max = 60) String sort) {
        if (principal == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED, "Authentication is required", HttpStatus.UNAUTHORIZED);
        }
        String ownerPublicId = BenefitControllerSupport.actor(principal);
        PagedResponse<VoucherResponse> data = voucherService.wallet(
                ownerPublicId, status,
                BenefitControllerSupport.pageable(page, size, sort, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
