package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.ForceReleaseRequest;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.promotion.service.CampaignEmergencyService;
import com.project.promotionservice.promotion.service.PromotionResourceScopeService;
import com.project.promotionservice.promotion.service.EmergencyCommandIdempotencyExecutor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/promotion-campaigns")
public class AdminCampaignEmergencyController {

    private final CampaignEmergencyService service;
    private final PromotionResourceScopeService resourceScope;
    private final EmergencyCommandIdempotencyExecutor idempotency;

    public AdminCampaignEmergencyController(
            CampaignEmergencyService service,
            PromotionResourceScopeService resourceScope,
            EmergencyCommandIdempotencyExecutor idempotency) {
        this.service = service;
        this.resourceScope = resourceScope;
        this.idempotency = idempotency;
    }

    @GetMapping("/{id}/force-release-impact")
    @PreAuthorize("hasAnyAuthority('PROMOTION_EMERGENCY_STOP','PROMOTION_FORCE_RELEASE')")
    public ResponseEntity<ApiResponse<ForceReleaseImpactResponse>> impact(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(publicId, principal);
        return ResponseEntity.ok(ApiResponse.success(service.impact(publicId)));
    }

    @PostMapping("/{id}/force-release")
    @PreAuthorize("hasAuthority('PROMOTION_FORCE_RELEASE')")
    public ResponseEntity<ApiResponse<ForceReleaseImpactResponse>> forceRelease(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @Valid @RequestBody ForceReleaseRequest request,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 255)
            String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(publicId, principal);
        String actor = SecurityActor.current();
        return ResponseEntity.ok(ApiResponse.success(
                "Active promotion holds released",
                idempotency.execute(actor, idempotencyKey, publicId, request,
                        () -> service.forceRelease(publicId, request.campaignCode(),
                                request.reason(), request.impactToken(),
                                request.campaignVersion(), idempotencyKey, actor))));
    }
}
