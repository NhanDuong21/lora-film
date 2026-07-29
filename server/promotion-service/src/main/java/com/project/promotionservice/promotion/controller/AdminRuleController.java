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

import java.util.Map;
import java.util.Set;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
import static com.project.promotionservice.common.web.ControllerPageSupport.pageable;

@RestController
@Validated
@RequestMapping("/api/admin/promotion-rules")
@Tag(name = "Admin Rule Management", description = "APIs for creating and configuring rules of campaigns")
public class AdminRuleController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "code", "name", "priority", "enabled");

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
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
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
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String actor = getActor(userPrincipal);
        ruleService.deleteRule(publicId, actor);
        return ResponseEntity.ok(new ApiResponse<>(true, "Promotion rule soft deleted successfully", null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Get rule details")
    public ResponseEntity<ApiResponse<RuleResponse>> getRule(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId) {
        RuleResponse data = ruleService.getRule(publicId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule retrieved successfully", data));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER', 'FINANCE_DIRECTOR')")
    @Operation(summary = "Search rules dynamically with pagination")
    public ResponseEntity<ApiResponse<PagedResponse<RuleResponse>>> searchRules(
            @RequestParam(value = "campaignPublicId", required = false)
            @Pattern(regexp = UUID_PATTERN, message = "campaignPublicId must be a valid UUID")
            String campaignPublicId,
            @RequestParam(value = "code", required = false) @Size(max = 50) String code,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") @Size(max = 60) String sort) {

        Pageable pageable = pageable(page, size, sort, SORT_FIELDS, "createdAt");
        PagedResponse<RuleResponse> data = ruleService.searchRules(campaignPublicId, code, enabled, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Rule search results retrieved", data));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MARKETING_MANAGER')")
    @Operation(summary = "Clone an existing rule into new campaign")
    public ResponseEntity<ApiResponse<RuleResponse>> cloneRule(
            @PathVariable("id")
            @Pattern(regexp = UUID_PATTERN, message = "id must be a valid UUID")
            String publicId,
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
