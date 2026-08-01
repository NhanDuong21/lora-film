package com.project.promotionservice.common.monitoring;

import com.project.promotionservice.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/promotion-monitoring")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'FINANCE_DIRECTOR')")
@Tag(name = "Promotion Operations Monitoring")
@SecurityRequirement(name = "bearerAuth")
public class AdminPromotionMonitoringController {

    private final PromotionOperationsMonitoringService monitoringService;

    public AdminPromotionMonitoringController(
            PromotionOperationsMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get promotion operations dashboard summary")
    public ResponseEntity<ApiResponse<PromotionOperationsSummary>> summary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Promotion monitoring summary retrieved successfully",
                monitoringService.getSummary()));
    }
}
