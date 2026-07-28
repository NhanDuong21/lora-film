package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherBatchIssueRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherExtendRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherIssueRequest;
import com.project.promotionservice.benefit.dto.request.VoucherRequests.VoucherUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.VoucherResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherSource;
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import com.project.promotionservice.benefit.service.VoucherService;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Vouchers", description = "Voucher issuance and lifecycle")
public class AdminVoucherController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "code", "name", "status", "source", "validFrom", "validTo");

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    @Operation(summary = "Issue voucher")
    public ResponseEntity<ApiResponse<VoucherResponse>> issue(
            @Valid @RequestBody VoucherIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        VoucherResponse data = voucherService.issue(request, BenefitControllerSupport.actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Voucher issued successfully", data));
    }

    @PostMapping("/batch")
    @Operation(summary = "Batch issue vouchers")
    public ResponseEntity<ApiResponse<List<VoucherResponse>>> batchIssue(
            @Valid @RequestBody VoucherBatchIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<VoucherResponse> data = voucherService.batchIssue(
                request, BenefitControllerSupport.actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Voucher batch issued successfully", data));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update voucher")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(
            @PathVariable("id") String publicId,
            @Valid @RequestBody VoucherUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Voucher updated successfully",
                voucherService.update(publicId, request, BenefitControllerSupport.actor(principal))));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke voucher")
    public ResponseEntity<ApiResponse<VoucherResponse>> revoke(
            @PathVariable("id") String publicId,
            @RequestParam(defaultValue = "Revoked by administrator") String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Voucher revoked successfully",
                voucherService.revoke(publicId, reason, BenefitControllerSupport.actor(principal))));
    }

    @PostMapping("/{id}/extend")
    @Operation(summary = "Extend voucher validity")
    public ResponseEntity<ApiResponse<VoucherResponse>> extend(
            @PathVariable("id") String publicId,
            @Valid @RequestBody VoucherExtendRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Voucher extended successfully",
                voucherService.extend(publicId, request, BenefitControllerSupport.actor(principal))));
    }

    @GetMapping
    @Operation(summary = "Search vouchers")
    public ResponseEntity<ApiResponse<PagedResponse<VoucherResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ownerPublicId,
            @RequestParam(required = false) String campaignPublicId,
            @RequestParam(required = false) VoucherStatus status,
            @RequestParam(required = false) VoucherSource source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        PagedResponse<VoucherResponse> data = voucherService.search(
                keyword, ownerPublicId, campaignPublicId, status, source,
                BenefitControllerSupport.pageable(page, size, sort, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get voucher detail")
    public ResponseEntity<ApiResponse<VoucherResponse>> get(@PathVariable("id") String publicId) {
        return ResponseEntity.ok(ApiResponse.success(voucherService.get(publicId)));
    }
}
