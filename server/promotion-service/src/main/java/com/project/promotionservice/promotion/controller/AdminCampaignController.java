package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.request.CampaignUpdateRequest;
import com.project.promotionservice.promotion.dto.request.ApprovalRequest;
import com.project.promotionservice.promotion.dto.request.LegalReviewRequest;
import com.project.promotionservice.promotion.dto.response.CampaignDetailResponse;
import com.project.promotionservice.promotion.dto.response.CampaignResponse;
import com.project.promotionservice.promotion.dto.response.ApprovalHistoryResponse;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import com.project.promotionservice.promotion.enums.CampaignTransitionAction;
import com.project.promotionservice.promotion.service.CampaignService;
import com.project.promotionservice.promotion.service.ApprovalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
import static com.project.promotionservice.common.web.ControllerPageSupport.pageable;

@RestController
@Validated
@RequestMapping("/api/admin/promotion-campaigns")
@Tag(name = "Admin Campaign Management", description = "APIs for managing and orchestrating promotion campaigns")
public class AdminCampaignController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "code", "name", "status", "startAt", "endAt", "priority");

    private final CampaignService campaignService;
    private final ApprovalService approvalService;

    public AdminCampaignController(CampaignService campaignService, ApprovalService approvalService) {
        this.campaignService = campaignService;
        this.approvalService = approvalService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_STAFF')")
    @Operation(summary = "Create a new campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        CampaignResponse data = campaignService.createCampaign(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Promotion campaign created successfully", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_STAFF')")
    @Operation(summary = "Update an existing campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @Valid @RequestBody CampaignUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        CampaignResponse data = campaignService.updateCampaign(publicId, request, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Promotion campaign updated successfully", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Soft delete a campaign")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        campaignService.deleteCampaign(publicId, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Promotion campaign soft deleted successfully", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_STAFF', 'FINANCE', 'FINANCE_DIRECTOR', 'LEGAL_COMPLIANCE', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Get campaign details (including rules)")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> getCampaign(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId) {
        CampaignDetailResponse data = campaignService.getCampaign(publicId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign retrieved successfully", data));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_STAFF', 'FINANCE', 'FINANCE_DIRECTOR', 'LEGAL_COMPLIANCE', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Search campaigns dynamically with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> searchCampaigns(
            @RequestParam(value = "name", required = false) @Size(max = 120) String name,
            @RequestParam(value = "code", required = false) @Size(max = 50) String code,
            @RequestParam(value = "status", required = false) CampaignStatus status,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") @Size(max = 60) String sort) {

        Pageable pageable = pageable(page, size, sort, SORT_FIELDS, "createdAt");
        PagedResponse<CampaignResponse> data =
                campaignService.searchCampaigns(name, code, status, from, to, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign search results retrieved", data));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Transition campaign status (SUBMIT, PUBLISH, ACTIVATE, PAUSE, KILL_SWITCH, CANCEL)")
    public ResponseEntity<ApiResponse<CampaignResponse>> transitionCampaignStatus(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @RequestParam("action") CampaignTransitionAction action,
            @RequestParam(value = "comment", required = false) @Size(max = 500) String comment,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        CampaignResponse data;
        switch (action) {
            case SUBMIT -> {
                String finalComment = comment != null ? comment : "Submitted for approval";
                data = campaignService.submitCampaign(publicId, finalComment, actor);
            }
            case PUBLISH -> data = campaignService.publishCampaign(publicId, actor);
            case ACTIVATE -> data = campaignService.activateCampaign(publicId, actor);
            case PAUSE -> data = campaignService.pauseCampaign(publicId, actor);
            case KILL_SWITCH -> data = campaignService.killSwitchCampaign(
                    publicId, comment, actor);
            case CANCEL -> data = campaignService.cancelCampaign(publicId, actor);
            default -> throw new IllegalArgumentException("Invalid status transition action: " + action);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign status transitioned to " + action + " successfully", data));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Approve a pending campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> approveCampaign(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String actor = getActor(userPrincipal);
        List<String> roles = userPrincipal != null ? userPrincipal.getRoles() : List.of();
        String comment = request != null ? request.getComment() : "Approved";

        CampaignResponse data = approvalService.approveCampaign(publicId, comment, actor, roles);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign approved successfully", data));
    }

    @PostMapping("/{id}/legal-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL_COMPLIANCE')")
    @Operation(summary = "Record the legal compliance decision for a campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> reviewLegalStatus(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @Valid @RequestBody LegalReviewRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CampaignResponse data = campaignService.reviewLegalStatus(
                publicId, request, getActor(userPrincipal));
        return ResponseEntity.ok(new ApiResponse<>(
                true, "Campaign legal review recorded successfully", data));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Reject a pending campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> rejectCampaign(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String actor = getActor(userPrincipal);
        List<String> roles = userPrincipal != null ? userPrincipal.getRoles() : List.of();
        String comment = request != null ? request.getComment() : "Rejected";

        CampaignResponse data = approvalService.rejectCampaign(publicId, comment, actor, roles);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign rejected successfully", data));
    }

    @GetMapping("/{id}/approval-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'MARKETING_STAFF', 'FINANCE', 'FINANCE_DIRECTOR', 'LEGAL_COMPLIANCE')")
    @Operation(summary = "Get approval history for campaign")
    public ResponseEntity<ApiResponse<List<ApprovalHistoryResponse>>> getApprovalHistory(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId) {
        List<ApprovalHistoryResponse> data = approvalService.getApprovalHistory(publicId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Approval histories retrieved successfully", data));
    }

    private String getActor(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return "SYSTEM";
        }
        if (userPrincipal.getId() != null) {
            return userPrincipal.getId().toString();
        }
        if (userPrincipal.getUsername() != null) {
            String username = userPrincipal.getUsername();
            return username.length() > 36 ? username.substring(0, 36) : username;
        }
        return "SYSTEM";
    }
}
