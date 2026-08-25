package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.CampaignPresentationUpdateRequest;
import com.project.promotionservice.promotion.dto.response.CampaignPresentationResponse;
import com.project.promotionservice.promotion.service.CampaignPresentationService;
import com.project.promotionservice.promotion.service.PromotionResourceScopeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/promotion-campaigns/{campaignId}/presentation")
public class AdminCampaignPresentationController {

    private final CampaignPresentationService service;
    private final PromotionResourceScopeService resourceScope;

    public AdminCampaignPresentationController(
            CampaignPresentationService service,
            PromotionResourceScopeService resourceScope) {
        this.service = service;
        this.resourceScope = resourceScope;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<CampaignPresentationResponse>> get(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "campaignId must be a valid UUID")
            String campaignId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(campaignId, principal);
        return ResponseEntity.ok(ApiResponse.success(service.get(campaignId)));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<CampaignPresentationResponse>> update(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "campaignId must be a valid UUID")
            String campaignId,
            @Valid @RequestBody CampaignPresentationUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(campaignId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign presentation saved as draft",
                service.update(campaignId, request, SecurityActor.current())));
    }

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<CampaignPresentationResponse>> uploadCover(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "campaignId must be a valid UUID")
            String campaignId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(campaignId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign cover uploaded",
                service.uploadCover(campaignId, file, SecurityActor.current())));
    }

    @DeleteMapping("/cover")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<CampaignPresentationResponse>> removeCover(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "campaignId must be a valid UUID")
            String campaignId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(campaignId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign cover removed",
                service.removeCover(campaignId, SecurityActor.current())));
    }

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('PROMOTION_PUBLISH')")
    public ResponseEntity<ApiResponse<CampaignPresentationResponse>> publish(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "campaignId must be a valid UUID")
            String campaignId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(campaignId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign presentation published",
                service.publish(campaignId, SecurityActor.current())));
    }

    @PostMapping("/unpublish")
    @PreAuthorize("hasAuthority('PROMOTION_PUBLISH')")
    public ResponseEntity<ApiResponse<CampaignPresentationResponse>> unpublish(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "campaignId must be a valid UUID")
            String campaignId,
            @AuthenticationPrincipal UserPrincipal principal) {
        resourceScope.requireCampaignAccess(campaignId, principal);
        return ResponseEntity.ok(ApiResponse.success(
                "Campaign presentation unpublished",
                service.unpublish(campaignId, SecurityActor.current())));
    }
}
