package com.project.promotionservice.controller;

import com.project.promotionservice.common.ApiResponse;
import com.project.promotionservice.dto.*;
import com.project.promotionservice.service.PromotionCampaignAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promotion-campaigns")
public class PromotionCampaignAdminController {

    private final PromotionCampaignAdminService campaignService;

    public PromotionCampaignAdminController(PromotionCampaignAdminService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'PROMOTION_CREATE')")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = campaignService.createCampaign(request);
        ApiResponse<CampaignResponse> apiResponse = ApiResponse.success("Promotion campaign created successfully", response);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }
}
