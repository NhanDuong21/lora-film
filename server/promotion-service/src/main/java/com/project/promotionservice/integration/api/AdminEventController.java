package com.project.promotionservice.integration.api;

import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.integration.job.JobExecutionService;
import com.project.promotionservice.integration.job.PromotionJobRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/admin/events")
@Tag(name = "Event Monitoring")
public class AdminEventController {
    private final IntegrationOperationsService events;
    private final JobExecutionService jobs;
    private final PromotionJobRunner runner;

    public AdminEventController(IntegrationOperationsService events, JobExecutionService jobs,
                                PromotionJobRunner runner) {
        this.events = events; this.jobs = jobs; this.runner = runner;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_MANAGER','FINANCE_DIRECTOR')")
    @Operation(summary = "Get event history")
    public ResponseEntity<ApiResponse<List<EventHistoryResponse>>> history(
            @RequestParam(defaultValue = "OUTBOUND") String direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(ApiResponse.success("Event history", events.history(direction, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_MANAGER','FINANCE_DIRECTOR')")
    @Operation(summary = "Get event detail")
    public ResponseEntity<ApiResponse<EventHistoryResponse>> detail(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{1,150}$") String id,
            @RequestParam(defaultValue = "OUTBOUND") String direction) {
        return ResponseEntity.ok(ApiResponse.success("Event detail", events.detail(id, direction)));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS_MANAGER')")
    @Operation(summary = "Get scheduler execution history")
    public ResponseEntity<ApiResponse<?>> jobs(@RequestParam(required = false) String jobName) {
        return ResponseEntity.ok(ApiResponse.success("Scheduler status",
                jobName == null ? runner.recentAll() : jobs.recent(jobName)));
    }

    @PostMapping("/jobs/{jobName}/run")
    @PreAuthorize("hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Run a scheduler job manually")
    public ResponseEntity<ApiResponse<Integer>> run(@PathVariable String jobName) {
        return ResponseEntity.ok(ApiResponse.success("Scheduler job executed",
                runner.run(jobName, "MANUAL", com.project.promotionservice.common.web.SecurityActor.current())));
    }
}
