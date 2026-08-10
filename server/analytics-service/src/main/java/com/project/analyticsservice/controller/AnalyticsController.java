package com.project.analyticsservice.controller;

import com.project.analyticsservice.application.AnalyticsQueryApplicationService;
import com.project.analyticsservice.application.AnalyticsJobApplicationService;
import com.project.analyticsservice.application.AnalyticsLifecycleApplicationService;
import com.project.analyticsservice.common.ApiResponse;
import com.project.analyticsservice.dto.AnalyticsCommands;
import com.project.analyticsservice.dto.AnalyticsResponses;
import com.project.analyticsservice.security.ManagerCinemaScopeService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Business intelligence and decision support APIs")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ROLE_ACCOUNTANT','PERM_VIEW_FINANCE',"
        + "'DASHBOARD_VIEW','ANALYTICS_VIEW','ANALYTICS_MANAGE','ANALYTICS_REBUILD')")
public class AnalyticsController {
    private final AnalyticsQueryApplicationService queryService;
    private final AnalyticsLifecycleApplicationService lifecycleService;
    private final AnalyticsJobApplicationService jobService;
    private final ManagerCinemaScopeService cinemaScope;

    public AnalyticsController(
            AnalyticsQueryApplicationService queryService,
            AnalyticsLifecycleApplicationService lifecycleService,
            AnalyticsJobApplicationService jobService,
            ManagerCinemaScopeService cinemaScope) {
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
        this.jobService = jobService;
        this.cinemaScope = cinemaScope;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get the complete analytics dashboard")
    public ApiResponse<AnalyticsResponses.Dashboard> dashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String cinemaKey) {
        cinemaScope.requireIfManager(cinemaKey);
        return ApiResponse.success(
                "Analytics dashboard retrieved successfully",
                queryService.dashboard(startDate, endDate, cinemaKey));
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<AnalyticsResponses.CinemaKpi>> cinemas(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer limit) {
        List<AnalyticsResponses.CinemaKpi> cinemas = queryService.cinemas(startDate, endDate, limit);
        if (cinemaScope.isManager()) {
            var assigned = cinemaScope.assignedCinemaKeys();
            cinemas = cinemas.stream()
                    .filter(cinema -> assigned.contains(cinema.cinemaKey().toLowerCase(java.util.Locale.ROOT)))
                    .toList();
        }
        return ApiResponse.success("Cinema KPIs retrieved successfully", cinemas);
    }

    @PatchMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ANALYTICS_MANAGE')")
    public ApiResponse<AnalyticsResponses.ActionResult> acknowledgeAlert(
            @PathVariable long id, Principal principal) {
        return ApiResponse.success(
                "Alert acknowledged successfully",
                lifecycleService.acknowledgeAlert(
                        id,
                        principal == null ? null : principal.getName(),
                        cinemaScope.isManager() ? cinemaScope.assignedCinemaKeys() : null));
    }

    @PatchMapping("/recommendations/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ANALYTICS_MANAGE')")
    public ApiResponse<AnalyticsResponses.ActionResult> updateRecommendation(
            @PathVariable long id,
            @Valid @RequestBody AnalyticsCommands.RecommendationStatus request,
            Principal principal) {
        return ApiResponse.success(
                "Recommendation updated successfully",
                lifecycleService.updateRecommendation(
                        id,
                        request.status(),
                        principal == null ? null : principal.getName(),
                        cinemaScope.isManager() ? cinemaScope.assignedCinemaKeys() : null));
    }

    @PostMapping("/jobs/rebuild")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ANALYTICS_REBUILD')")
    public ApiResponse<AnalyticsResponses.Job> rebuild(
            @Valid @RequestBody AnalyticsCommands.RebuildJob request,
            Principal principal) {
        return ApiResponse.success(
                "Analytics rebuild queued successfully",
                jobService.submit(request, principal == null ? null : principal.getName()));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ANALYTICS_REBUILD')")
    public ApiResponse<List<AnalyticsResponses.Job>> jobs() {
        return ApiResponse.success(
                "Analytics jobs retrieved successfully",
                jobService.recent());
    }
}
