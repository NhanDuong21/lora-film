package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.PromotionIssueRequest;
import com.project.promotionservice.promotion.dto.request.PromotionUpsertRequest;
import com.project.promotionservice.promotion.dto.response.PromotionCloneDraftResponse;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.dto.response.PromotionResponse;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.PromotionDistributionMode;
import com.project.promotionservice.promotion.service.PromotionCatalogService;
import com.project.promotionservice.promotion.service.PromotionResourceScopeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;
import static com.project.promotionservice.common.web.ControllerPageSupport.pageable;

@RestController
@Validated
@RequestMapping("/api/admin/promotions")
public class AdminPromotionController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "name", "code", "priority",
            "promotionType", "status", "validFrom", "validTo");

    private final PromotionCatalogService service;
    private final PromotionResourceScopeService resourceScope;

    public AdminPromotionController(
            PromotionCatalogService service,
            PromotionResourceScopeService resourceScope) {
        this.service = service;
        this.resourceScope = resourceScope;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PromotionResponse>> create(
            @Valid @RequestBody PromotionUpsertRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionTargetAllowed(
                request.campaignPublicId(), request.conditionsJson(), principal);
        PromotionResponse data = service.create(request, SecurityActor.current());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion created", data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PromotionResponse>> update(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @Valid @RequestBody PromotionUpsertRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        resourceScope.requirePromotionTargetAllowed(
                request.campaignPublicId(), request.conditionsJson(), principal);
        return ResponseEntity.ok(ApiResponse.success("Promotion updated",
                service.update(publicId, request, SecurityActor.current())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<PromotionResponse>> detail(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        return ResponseEntity.ok(ApiResponse.success(service.detail(publicId)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<PagedResponse<PromotionResponse>>> search(
            @RequestParam(required = false) @Size(max = 36) String campaignPublicId,
            @RequestParam(required = false) PromotionType type,
            @RequestParam(required = false) PromotionStatus status,
            @RequestParam(required = false) Boolean publicVisible,
            @RequestParam(required = false) Set<PromotionDistributionMode> distributionMode,
            @RequestParam(required = false) Boolean testData,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") @Size(max = 60) String sort,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (campaignPublicId != null && !campaignPublicId.isBlank()) {
            resourceScope.requireCampaignAccess(campaignPublicId, principal);
        }
        Set<String> accessibleCampaignIds = resourceScope.isManager(principal)
                ? resourceScope.accessibleCampaignIds(principal)
                : null;
        return ResponseEntity.ok(ApiResponse.success(service.search(
                campaignPublicId, type, status, publicVisible, keyword, distributionMode,
                testData, pageable(page, size, sort, SORT_FIELDS, "createdAt"),
                accessibleCampaignIds)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PromotionResponse>> activate(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        return ResponseEntity.ok(ApiResponse.success("Promotion activated",
                service.activate(publicId, SecurityActor.current())));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('PROMOTION_OPERATE')")
    public ResponseEntity<ApiResponse<PromotionResponse>> pause(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        return ResponseEntity.ok(ApiResponse.success("Promotion paused",
                service.pause(publicId, SecurityActor.current())));
    }

    @GetMapping("/{id}/clone-draft")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PromotionCloneDraftResponse>> cloneDraft(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        return ResponseEntity.ok(ApiResponse.success(service.buildCloneDraft(publicId)));
    }

    @Deprecated(forRemoval = true)
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PromotionResponse>> clonePromotion(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion cloned",
                        service.clonePromotion(publicId, SecurityActor.current())));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PromotionIssueResponse>> issue(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @Valid @RequestBody PromotionIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        return ResponseEntity.ok(ApiResponse.success("Promotion issued",
                service.issue(publicId, request.userPublicIds(), SecurityActor.current())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requirePromotionAccess(publicId, principal);
        service.delete(publicId, SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Promotion deleted", null));
    }
}
