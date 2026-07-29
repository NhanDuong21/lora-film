package com.project.promotionservice.integration.api;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.web.SecurityActor;
import com.project.promotionservice.integration.job.PromotionJobRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal scheduler hooks used by the platform scheduler.  Keeping these
 * endpoints separate from the admin monitoring API prevents service-to-service
 * callers from receiving an admin session or having to know implementation
 * specific job names.
 */
@RestController
@RequestMapping("/internal/schedulers")
@PreAuthorize("hasRole('OPERATIONS_SERVICE')")
@Tag(name = "Internal Scheduler Operations")
public class InternalSchedulerController {

    private final PromotionJobRunner jobs;

    public InternalSchedulerController(PromotionJobRunner jobs) {
        this.jobs = jobs;
    }

    @PostMapping("/campaigns/activate")
    @Operation(summary = "Activate approved scheduled campaigns that reached their start time")
    public ResponseEntity<ApiResponse<Integer>> activateCampaigns() {
        return run("campaigns-activate");
    }

    @PostMapping("/campaigns/expire")
    @Operation(summary = "Expire campaigns that reached their end time")
    public ResponseEntity<ApiResponse<Integer>> expireCampaigns() {
        return run("campaigns-expire");
    }

    @PostMapping("/coupons/expire")
    @Operation(summary = "Expire coupons that reached their validity end")
    public ResponseEntity<ApiResponse<Integer>> expireCoupons() {
        return run("coupons-expire");
    }

    @PostMapping("/vouchers/expire")
    @Operation(summary = "Expire vouchers that reached their validity end")
    public ResponseEntity<ApiResponse<Integer>> expireVouchers() {
        return run("vouchers-expire");
    }

    @PostMapping("/outbox/publish")
    @Operation(summary = "Publish pending transactional outbox events")
    public ResponseEntity<ApiResponse<Integer>> publishOutbox() {
        return run("outbox-publish");
    }

    @PostMapping("/outbox/retry")
    @Operation(summary = "Make failed outbox events eligible for retry")
    public ResponseEntity<ApiResponse<Integer>> retryOutbox() {
        return run("outbox-retry");
    }

    @PostMapping("/cache/refresh")
    @Operation(summary = "Refresh runtime configuration cache")
    public ResponseEntity<ApiResponse<Integer>> refreshCache() {
        return run("cache-refresh");
    }

    private ResponseEntity<ApiResponse<Integer>> run(String jobName) {
        int processed = jobs.run(jobName, "INTERNAL", SecurityActor.current());
        return ResponseEntity.ok(ApiResponse.success("Scheduler job executed", processed));
    }
}
