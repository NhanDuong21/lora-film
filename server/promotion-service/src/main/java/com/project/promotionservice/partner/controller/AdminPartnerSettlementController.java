package com.project.promotionservice.partner.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.web.ControllerPageSupport;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.partner.dto.request.SettlementCreateRequest;
import com.project.promotionservice.partner.dto.request.SettlementUpdateRequest;
import com.project.promotionservice.partner.dto.response.SettlementResponse;
import com.project.promotionservice.partner.enums.SettlementStatus;
import com.project.promotionservice.partner.service.PartnerSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/partner-settlements")
@Tag(name = "Partner Settlement Management")
public class AdminPartnerSettlementController {
    private static final java.util.Set<String> SORT_FIELDS =
            java.util.Set.of("createdAt", "updatedAt", "settlementPeriodFrom", "settlementPeriodTo",
                    "totalDiscount", "partnerAmount", "status");
    private final PartnerSettlementService service;

    public AdminPartnerSettlementController(PartnerSettlementService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR')")
    @Operation(summary = "Create settlement snapshot from confirmed redemptions")
    public ResponseEntity<ApiResponse<SettlementResponse>> create(
            @Valid @RequestBody SettlementCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Settlement created successfully", service.create(request, SecurityActor.current())));
    }

    @PutMapping({"/{id}", "/{id}/status"})
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR')")
    @Operation(summary = "Update settlement status or adjustment")
    public ResponseEntity<ApiResponse<SettlementResponse>> update(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @Valid @RequestBody SettlementUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Settlement updated successfully",
                service.update(id, request, SecurityActor.current())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR')")
    @Operation(summary = "Disable settlement")
    public ResponseEntity<ApiResponse<Void>> disable(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        service.disable(id, SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Settlement disabled successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR')")
    @Operation(summary = "Search settlements")
    public ResponseEntity<ApiResponse<PagedResponse<SettlementResponse>>> search(
            @RequestParam(required = false) @Pattern(regexp = UUID_PATTERN) String partnerPublicId,
            @RequestParam(required = false) @Pattern(regexp = UUID_PATTERN) String campaignPublicId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") @Size(max = 60) String sort) {
        Pageable pageable = ControllerPageSupport.pageable(page, size, sort, SORT_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResponse.success("Settlement search results",
                service.search(partnerPublicId, campaignPublicId, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR')")
    @Operation(summary = "Get settlement detail")
    public ResponseEntity<ApiResponse<SettlementResponse>> detail(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success("Settlement detail", service.detail(id)));
    }
}
