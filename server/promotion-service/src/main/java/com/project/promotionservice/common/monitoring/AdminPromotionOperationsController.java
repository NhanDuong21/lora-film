package com.project.promotionservice.common.monitoring;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.service.PromotionResourceScopeService;
import com.project.promotionservice.reservation.enums.ReleaseReasonType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/admin/promotion-operations")
@PreAuthorize("hasAuthority('PROMOTION_AUDIT_VIEW')")
public class AdminPromotionOperationsController {

    private final PromotionOperationsSearchService service;
    private final PromotionResourceScopeService resourceScope;

    public AdminPromotionOperationsController(
            PromotionOperationsSearchService service,
            PromotionResourceScopeService resourceScope) {
        this.service = service;
        this.resourceScope = resourceScope;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PromotionOperationsSearchResponse>> search(
            @RequestParam(required = false) @Size(max = 150) String query,
            @RequestParam(required = false) @Size(max = 36) String campaignPublicId,
            @RequestParam(required = false) @Size(max = 36) String promotionPublicId,
            @RequestParam(required = false) @Size(max = 36) String reservationPublicId,
            @RequestParam(required = false) @Size(max = 36) String bookingPublicId,
            @RequestParam(required = false) @Size(max = 36) String paymentPublicId,
            @RequestParam(required = false) @Size(max = 50) String customerReference,
            @RequestParam(required = false) ReleaseReasonType releaseReasonType,
            @RequestParam(required = false) @Size(max = 40) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (campaignPublicId != null && !campaignPublicId.isBlank()) {
            resourceScope.requireCampaignAccess(campaignPublicId, principal);
        }
        if (promotionPublicId != null && !promotionPublicId.isBlank()) {
            resourceScope.requirePromotionAccess(promotionPublicId, principal);
        }
        Set<String> accessibleCampaignIds = resourceScope.isManager(principal)
                ? resourceScope.accessibleCampaignIds(principal)
                : null;
        return ResponseEntity.ok(ApiResponse.success(service.search(
                query, campaignPublicId, promotionPublicId, reservationPublicId,
                bookingPublicId, paymentPublicId, customerReference,
                releaseReasonType, status, from, to, limit,
                accessibleCampaignIds)));
    }
}
