package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.RuleCreateRequest;
import com.project.promotionservice.promotion.dto.request.RuleUpdateRequest;
import com.project.promotionservice.promotion.dto.request.RuleCloneRequest;
import com.project.promotionservice.promotion.dto.response.RuleResponse;
import com.project.promotionservice.promotion.service.RuleService;

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

import java.util.Map;

@RestController
@RequestMapping("/api/admin/promotion-rules")
@Tag(name = "Admin Rule Management", description = "APIs for creating and configuring rules of campaigns")
public class AdminRuleController {

    private final RuleService ruleService;

    public AdminRuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Create a rule for a campaign")
    public ResponseEntity<ApiResponse<RuleResponse>> createRule(
            @Valid @RequestBody RuleCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        RuleResponse data = ruleService.createRule(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Promotion rule created successfully", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Update an existing rule details")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(
            @PathVariable("id") String publicId,
            @Valid @RequestBody RuleUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        RuleResponse data = ruleService.updateRule(publicId, request, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Promotion rule updated successfully", data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Soft delete a rule")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable("id") String publicId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        ruleService.deleteRule(publicId, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Promotion rule soft deleted successfully", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Get rule details")
    public ResponseEntity<ApiResponse<RuleResponse>> getRule(@PathVariable("id") String publicId) {
        RuleResponse data = ruleService.getRule(publicId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule retrieved successfully", data));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Search rules dynamically with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<RuleResponse>>> searchRules(
            @RequestParam(value = "campaignPublicId", required = false) String campaignPublicId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort) {

        String[] sortParams = sort.split(",");
        Sort sortOrder = Sort.by(sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) ? Sort.Direction.ASC : Sort.Direction.DESC, sortParams[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        PagedResponse<RuleResponse> data = ruleService.searchRules(campaignPublicId, code, enabled, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule search results retrieved", data));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Clone an existing rule into new campaign")
    public ResponseEntity<ApiResponse<RuleResponse>> cloneRule(
            @PathVariable("id") String publicId,
            @Valid @RequestBody RuleCloneRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        RuleResponse data = ruleService.cloneRule(publicId, request, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule cloned successfully", data));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Simulate a preview discount based on rules and context metadata")
    public ResponseEntity<ApiResponse<Double>> previewDiscount(
            @RequestBody Map<String, String> payload) {
        String conditions = payload.get("conditionsJson");
        String actions = payload.get("actionsJson");
        String context = payload.get("contextJson");
        double data = ruleService.previewDiscount(conditions, actions, context);
        return ResponseEntity.ok(new ApiResponse<>(true, "Simulated preview discount calculated successfully", data));
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
