package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.CampaignCreateRequest;
import com.project.promotionservice.promotion.dto.request.CampaignUpdateRequest;
import com.project.promotionservice.promotion.dto.request.ApprovalRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/promotion-campaigns")
@Tag(name = "Admin Campaign Management", description = "APIs for managing and orchestrating promotion campaigns")
public class AdminCampaignController {

    private final CampaignService campaignService;
    private final ApprovalService approvalService;

    public AdminCampaignController(CampaignService campaignService, ApprovalService approvalService) {
        this.campaignService = campaignService;
        this.approvalService = approvalService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Update an existing campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable("id") String publicId,
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
            @PathVariable("id") String publicId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        campaignService.deleteCampaign(publicId, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Promotion campaign soft deleted successfully", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Get campaign details (including rules)")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> getCampaign(
            @PathVariable("id") String publicId) {
        CampaignDetailResponse data = campaignService.getCampaign(publicId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign retrieved successfully", data));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Search campaigns dynamically with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<CampaignResponse>>> searchCampaigns(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "status", required = false) CampaignStatus status,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) ? Sort.Direction.ASC : Sort.Direction.DESC, sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        PagedResponse<CampaignResponse> data = campaignService.searchCampaigns(name, code, status, from, to, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign search results retrieved", data));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Transition campaign status (SUBMIT, PUBLISH, ACTIVATE, PAUSE, CANCEL)")
    public ResponseEntity<ApiResponse<CampaignResponse>> transitionCampaignStatus(
            @PathVariable("id") String publicId,
            @RequestParam("action") CampaignTransitionAction action,
            @RequestParam(value = "comment", required = false) String comment,
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
            case CANCEL -> data = campaignService.cancelCampaign(publicId, actor);
            default -> throw new IllegalArgumentException("Invalid status transition action: " + action);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign status transitioned to " + action + " successfully", data));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Approve a pending campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> approveCampaign(
            @PathVariable("id") String publicId,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String actor = getActor(userPrincipal);
        List<String> roles = userPrincipal != null ? userPrincipal.getRoles() : List.of();
        String comment = request != null ? request.getComment() : "Approved";

        CampaignResponse data = approvalService.approveCampaign(publicId, comment, actor, roles);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign approved successfully", data));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Reject a pending campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> rejectCampaign(
            @PathVariable("id") String publicId,
            @Valid @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        String actor = getActor(userPrincipal);
        List<String> roles = userPrincipal != null ? userPrincipal.getRoles() : List.of();
        String comment = request != null ? request.getComment() : "Rejected";

        CampaignResponse data = approvalService.rejectCampaign(publicId, comment, actor, roles);
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign rejected successfully", data));
    }

    @GetMapping("/{id}/approval-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Get approval history for campaign")
    public ResponseEntity<ApiResponse<List<ApprovalHistoryResponse>>> getApprovalHistory(
            @PathVariable("id") String publicId) {
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
