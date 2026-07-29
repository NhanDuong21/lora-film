package com.project.analyticsservice.controller;

import com.project.analyticsservice.application.AnalyticsQueryApplicationService;
import com.project.analyticsservice.application.AnalyticsJobApplicationService;
import com.project.analyticsservice.application.AnalyticsLifecycleApplicationService;
import com.project.analyticsservice.common.ApiResponse;
import com.project.analyticsservice.dto.AnalyticsCommands;
import com.project.analyticsservice.dto.AnalyticsResponses;
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
        + "'DASHBOARD_VIEW','ANALYTICS_MANAGE','ANALYTICS_REBUILD')")
public class AnalyticsController {
    private final AnalyticsQueryApplicationService queryService;
    private final AnalyticsLifecycleApplicationService lifecycleService;
    private final AnalyticsJobApplicationService jobService;

    public AnalyticsController(
            AnalyticsQueryApplicationService queryService,
            AnalyticsLifecycleApplicationService lifecycleService,
            AnalyticsJobApplicationService jobService) {
        this.queryService = queryService;
        this.lifecycleService = lifecycleService;
        this.jobService = jobService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get the complete analytics dashboard")
    public ApiResponse<AnalyticsResponses.Dashboard> dashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(
                "Analytics dashboard retrieved successfully",
                queryService.dashboard(startDate, endDate));
    }

    @GetMapping("/daily")
    public ApiResponse<List<AnalyticsResponses.DailyKpi>> daily(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(
                "Daily KPIs retrieved successfully",
                queryService.daily(startDate, endDate));
    }

    @GetMapping("/cinemas")
    public ApiResponse<List<AnalyticsResponses.CinemaKpi>> cinemas(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                "Cinema KPIs retrieved successfully",
                queryService.cinemas(startDate, endDate, limit));
    }

    @GetMapping("/movie-performance")
    public ApiResponse<List<AnalyticsResponses.MovieKpi>> movies(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                "Movie KPIs retrieved successfully",
                queryService.movies(startDate, endDate, limit));
    }

    @GetMapping("/promotions")
    public ApiResponse<List<AnalyticsResponses.PromotionKpi>> promotions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(
                "Promotion KPIs retrieved successfully",
                queryService.promotions(startDate, endDate, limit));
    }

    @GetMapping("/customer-segments")
    public ApiResponse<List<AnalyticsResponses.CustomerSegment>> customerSegments(
            @RequestParam(required = false) String date) {
        return ApiResponse.success(
                "Customer segment KPIs retrieved successfully",
                queryService.customerSegments(date));
    }

    @GetMapping("/forecasts")
    public ApiResponse<List<AnalyticsResponses.Forecast>> forecasts(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(
                "Forecasts retrieved successfully",
                queryService.forecasts(startDate, endDate));
    }

    @GetMapping("/insights")
    public ApiResponse<List<AnalyticsResponses.Insight>> insights(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(
                "Insights retrieved successfully",
                queryService.insights(startDate, endDate));
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<AnalyticsResponses.Recommendation>> recommendations() {
        return ApiResponse.success(
                "Recommendations retrieved successfully",
                queryService.recommendations());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AnalyticsResponses.Alert>> alerts() {
        return ApiResponse.success(
                "Alerts retrieved successfully",
                queryService.alerts());
    }

    @GetMapping("/data-quality")
    public ApiResponse<AnalyticsResponses.DataQuality> dataQuality() {
        return ApiResponse.success(
                "Analytics data quality retrieved successfully",
                queryService.dataQuality());
    }

    @GetMapping("/health-score")
    public ApiResponse<AnalyticsResponses.HealthScore> healthScore(
            @RequestParam(required = false) String date) {
        return ApiResponse.success(
                "Analytics health score retrieved successfully",
                queryService.healthScore(date));
    }

    @GetMapping("/anomalies")
    public ApiResponse<List<AnalyticsResponses.Anomaly>> anomalies(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(
                "Analytics anomalies retrieved successfully",
                queryService.anomalies(startDate, endDate));
    }

    @GetMapping("/forecast-quality")
    public ApiResponse<List<AnalyticsResponses.ForecastQuality>> forecastQuality(
            @RequestParam(required = false) String date) {
        return ApiResponse.success(
                "Forecast quality retrieved successfully",
                queryService.forecastQuality(date));
    }

    @PatchMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER','ANALYTICS_MANAGE')")
    public ApiResponse<AnalyticsResponses.ActionResult> acknowledgeAlert(
            @PathVariable long id, Principal principal) {
        return ApiResponse.success(
                "Alert acknowledged successfully",
                lifecycleService.acknowledgeAlert(id, principal == null ? null : principal.getName()));
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
                        id, request.status(), principal == null ? null : principal.getName()));
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
