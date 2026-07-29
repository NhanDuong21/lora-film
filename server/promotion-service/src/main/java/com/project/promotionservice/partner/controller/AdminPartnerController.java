package com.project.promotionservice.partner.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.web.ControllerPageSupport;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.partner.dto.request.PartnerCreateRequest;
import com.project.promotionservice.partner.dto.request.PartnerUpdateRequest;
import com.project.promotionservice.partner.dto.response.PartnerResponse;
import com.project.promotionservice.partner.enums.PartnerStatus;
import com.project.promotionservice.partner.service.PartnerService;
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
@RequestMapping("/api/admin/partners")
@Tag(name = "Partner Management")
public class AdminPartnerController {
    private static final java.util.Set<String> SORT_FIELDS =
            java.util.Set.of("createdAt", "updatedAt", "code", "name", "status", "partnerType");
    private final PartnerService service;

    public AdminPartnerController(PartnerService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR','PARTNER_MANAGER')")
    @Operation(summary = "Create partner")
    public ResponseEntity<ApiResponse<PartnerResponse>> create(@Valid @RequestBody PartnerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Partner created successfully", service.create(request, SecurityActor.current())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR','PARTNER_MANAGER')")
    @Operation(summary = "Update partner")
    public ResponseEntity<ApiResponse<PartnerResponse>> update(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @Valid @RequestBody PartnerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Partner updated successfully",
                service.update(id, request, SecurityActor.current())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR','PARTNER_MANAGER')")
    @Operation(summary = "Disable partner (soft delete)")
    public ResponseEntity<ApiResponse<Void>> disable(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        service.disable(id, SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Partner disabled successfully", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR','PARTNER_MANAGER')")
    @Operation(summary = "Search partners")
    public ResponseEntity<ApiResponse<PagedResponse<PartnerResponse>>> search(
            @RequestParam(required = false) @Size(max = 150) String keyword,
            @RequestParam(required = false) PartnerStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") @Size(max = 60) String sort) {
        Pageable pageable = ControllerPageSupport.pageable(page, size, sort, SORT_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResponse.success("Partner search results", service.search(keyword, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_DIRECTOR','PARTNER_MANAGER')")
    @Operation(summary = "Get partner detail")
    public ResponseEntity<ApiResponse<PartnerResponse>> detail(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success("Partner detail", service.detail(id)));
    }
}
