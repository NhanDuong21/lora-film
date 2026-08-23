package com.project.promotionservice.promotion.manager;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import com.project.promotionservice.promotion.dto.request.PromotionIssueRequest;
import com.project.promotionservice.promotion.dto.response.PromotionIssueResponse;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Automation;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Benefit;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Campaign;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Incident;
import com.project.promotionservice.promotion.manager.ManagerPromotionDtos.Workspace;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/manager/promotions")
@PreAuthorize("hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW')")
public class ManagerPromotionController {
    private final ManagerPromotionWorkspaceService service;

    public ManagerPromotionController(ManagerPromotionWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/workspace")
    public ResponseEntity<ApiResponse<Workspace>> workspace(
            @RequestParam(required = false) @Size(max = 36) String cinemaPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.workspace(cinemaPublicId, principal)));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<List<Campaign>>> campaigns(
            @RequestParam(required = false) @Size(max = 36) String cinemaPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.campaigns(cinemaPublicId, principal)));
    }

    @GetMapping("/automations")
    public ResponseEntity<ApiResponse<List<Automation>>> automations(
            @RequestParam(required = false) @Size(max = 36) String cinemaPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.automations(cinemaPublicId, principal)));
    }

    @GetMapping("/distribution-options")
    public ResponseEntity<ApiResponse<List<Benefit>>> distributionOptions(
            @RequestParam(required = false) @Size(max = 36) String cinemaPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.distributionOptions(cinemaPublicId, principal)));
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW') and hasAuthority('PROMOTION_AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<List<Incident>>> incidents(
            @RequestParam(required = false) @Size(max = 36) String cinemaPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                service.incidents(cinemaPublicId, principal)));
    }

    @PostMapping("/distribution-options/{promotionId}/issue")
    @PreAuthorize("hasRole('MANAGER') and hasAuthority('PROMOTION_VIEW') and hasAuthority('PROMOTION_DISTRIBUTE_LOCAL')")
    public ResponseEntity<ApiResponse<PromotionIssueResponse>> issue(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String promotionId,
            @RequestParam(required = false) @Size(max = 36) String cinemaPublicId,
            @Valid @RequestBody PromotionIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Local benefit issued",
                service.issue(cinemaPublicId, promotionId,
                        request.userPublicIds(), principal)));
    }
}
