package com.project.promotionservice.promotion.controller;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.promotion.dto.request.ForceReleaseRequest;
import com.project.promotionservice.promotion.dto.response.ForceReleaseImpactResponse;
import com.project.promotionservice.promotion.service.CampaignEmergencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin/promotion-campaigns")
@PreAuthorize("hasAuthority('PROMOTION_EMERGENCY_STOP')")
public class AdminCampaignEmergencyController {

    private final CampaignEmergencyService service;

    public AdminCampaignEmergencyController(CampaignEmergencyService service) {
        this.service = service;
    }

    @GetMapping("/{id}/force-release-impact")
    public ResponseEntity<ApiResponse<ForceReleaseImpactResponse>> impact(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId) {
        return ResponseEntity.ok(ApiResponse.success(service.impact(publicId)));
    }

    @PostMapping("/{id}/force-release")
    public ResponseEntity<ApiResponse<ForceReleaseImpactResponse>> forceRelease(
            @PathVariable("id") @Pattern(regexp = UUID_PATTERN) String publicId,
            @Valid @RequestBody ForceReleaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Active promotion holds released",
                service.forceRelease(publicId, request.campaignCode(),
                        request.reason(), SecurityActor.current())));
    }
}
