package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.AdminMembershipTierResponse;
import com.project.scoreservice.dto.CreateMembershipTierRequest;
import com.project.scoreservice.dto.UpdateMembershipTierRequest;
import com.project.scoreservice.service.MembershipTierAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/membership-tiers")
@Tag(name = "Admin Membership Tier Management", description = "Endpoints for administrator's membership tiers CRUD operations")
public class AdminMembershipTierController {

    private final MembershipTierAdminService membershipTierAdminService;

    public AdminMembershipTierController(MembershipTierAdminService membershipTierAdminService) {
        this.membershipTierAdminService = membershipTierAdminService;
    }

    @PostMapping
    @Operation(summary = "Create membership tier", description = "Create a new membership tier configuration.")
    public ResponseEntity<ApiResponse<AdminMembershipTierResponse>> createTier(
            @Valid @RequestBody CreateMembershipTierRequest request) {
        AdminMembershipTierResponse response = membershipTierAdminService.createTier(request);
        return new ResponseEntity<>(ApiResponse.success("Membership tier created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all membership tiers", description = "Retrieve list of all configured membership tiers sorted by minimum points ascending.")
    public ResponseEntity<ApiResponse<List<AdminMembershipTierResponse>>> getTiers() {
        List<AdminMembershipTierResponse> response = membershipTierAdminService.getTiers();
        return ResponseEntity.ok(ApiResponse.success("Membership tiers retrieved successfully", response));
    }

    @GetMapping("/{tierId}")
    @Operation(summary = "Get membership tier detail", description = "Retrieve detail of a membership tier configuration by ID or code.")
    public ResponseEntity<ApiResponse<AdminMembershipTierResponse>> getTierDetail(
            @PathVariable("tierId") String tierIdOrCode) {
        AdminMembershipTierResponse response = membershipTierAdminService.getTierDetail(tierIdOrCode);
        return ResponseEntity.ok(ApiResponse.success("Membership tier retrieved successfully", response));
    }

    @PutMapping("/{tierId}")
    @Operation(summary = "Update membership tier", description = "Update an existing membership tier configuration by ID or code.")
    public ResponseEntity<ApiResponse<AdminMembershipTierResponse>> updateTier(
            @PathVariable("tierId") String tierIdOrCode,
            @Valid @RequestBody UpdateMembershipTierRequest request) {
        AdminMembershipTierResponse response = membershipTierAdminService.updateTier(tierIdOrCode, request);
        return ResponseEntity.ok(ApiResponse.success("Membership tier updated successfully", response));
    }
}
