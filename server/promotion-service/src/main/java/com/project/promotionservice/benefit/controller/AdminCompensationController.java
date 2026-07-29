package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationIssueRequest;
import com.project.promotionservice.benefit.dto.request.CompensationRequests.CompensationUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CompensationResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationStatus;
import com.project.promotionservice.benefit.enums.BenefitEnums.CompensationType;
import com.project.promotionservice.benefit.service.CompensationService;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
import static com.project.promotionservice.common.constant.ValidationConstants.USER_REFERENCE_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/compensation-vouchers")
@PreAuthorize("hasAnyRole('ADMIN', 'CSKH_AGENT')")
@Tag(name = "Admin Compensation Vouchers", description = "Audited customer compensation issuance")
public class AdminCompensationController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "issuedAt", "expiredAt", "status", "amount");

    private final CompensationService compensationService;

    public AdminCompensationController(CompensationService compensationService) {
        this.compensationService = compensationService;
    }

    @PostMapping
    @Operation(summary = "Approve and issue compensation voucher")
    public ResponseEntity<ApiResponse<CompensationResponse>> issue(
            @Valid @RequestBody CompensationIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CompensationResponse data = compensationService.issue(
                request, BenefitControllerSupport.actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Compensation voucher issued successfully", data));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update or cancel compensation voucher")
    public ResponseEntity<ApiResponse<CompensationResponse>> update(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @Valid @RequestBody CompensationUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Compensation voucher updated successfully",
                compensationService.update(
                        publicId, request, BenefitControllerSupport.actor(principal))));
    }

    @GetMapping
    @Operation(summary = "Search compensation vouchers")
    public ResponseEntity<ApiResponse<PagedResponse<CompensationResponse>>> search(
            @RequestParam(required = false)
            @Pattern(regexp = USER_REFERENCE_PATTERN,
                    message = "userPublicId must be a positive account ID or a valid UUID")
            String userPublicId,
            @RequestParam(required = false) CompensationType type,
            @RequestParam(required = false) CompensationStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "issuedAt,desc") @Size(max = 60) String sort) {
        PagedResponse<CompensationResponse> data = compensationService.search(
                userPublicId, type, status, from, to,
                BenefitControllerSupport.pageable(page, size, sort, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get compensation voucher detail and approval history")
    public ResponseEntity<ApiResponse<CompensationResponse>> get(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId) {
        return ResponseEntity.ok(ApiResponse.success(compensationService.get(publicId)));
    }
}
