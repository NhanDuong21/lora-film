package com.lorafilm.movie.autoschedule.controller;

import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.request.CheckAutoSchedulePricingRequest;
import com.lorafilm.movie.autoschedule.dto.request.CancelShowtimeSchedulePreviewRequest;
import com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreviewHistoryItemResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewHistoryService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewPricingReadinessService;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import com.lorafilm.movie.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/admin/showtime-schedules")
@Tag(name = "Admin Auto Showtime Scheduling API", description = "Admin endpoints for reviewing persisted auto scheduling previews")
public class AdminShowtimeScheduleController {

    private final ShowtimeSchedulePreviewService service;
    private final AutoSchedulePreviewHistoryService historyService;
    private final com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewGenerationService generationService;
    private final com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewApplyService applyService;
    private final com.lorafilm.movie.common.security.CurrentUserProvider currentUserProvider;
    private final com.lorafilm.movie.autoschedule.service.AutoScheduleEligibilityService eligibilityService;
    private final AutoSchedulePreviewPricingReadinessService pricingReadinessService;

    public AdminShowtimeScheduleController(ShowtimeSchedulePreviewService service,
                                           AutoSchedulePreviewHistoryService historyService,
                                           com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewGenerationService generationService,
                                           com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewApplyService applyService,
                                           com.lorafilm.movie.common.security.CurrentUserProvider currentUserProvider,
                                           com.lorafilm.movie.autoschedule.service.AutoScheduleEligibilityService eligibilityService,
                                           AutoSchedulePreviewPricingReadinessService pricingReadinessService) {
        this.service = service;
        this.historyService = historyService;
        this.generationService = generationService;
        this.applyService = applyService;
        this.currentUserProvider = currentUserProvider;
        this.eligibilityService = eligibilityService;
        this.pricingReadinessService = pricingReadinessService;
    }

    @Operation(summary = "Get preview history", description = "Retrieves a filtered page of auto schedule preview summaries without loading preview items.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview history retrieved"),
        @ApiResponse(responseCode = "400", description = "Invalid filter, pagination, or sort"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<PageResponse<AutoSchedulePreviewHistoryItemResponse>>> getPreviewHistory(
            @Valid @ParameterObject AutoSchedulePreviewHistoryQuery query
    ) {
        PageResponse<AutoSchedulePreviewHistoryItemResponse> response = historyService.getHistory(query);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok(
                "Auto schedule preview history retrieved successfully",
                response
        ));
    }

    @Operation(summary = "Get preview details", description = "Retrieves the persisted auto schedule preview and its candidates.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Preview not found")
    })
    @GetMapping("/{previewPublicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<ShowtimeSchedulePreviewPageResponse>> getPreview(
            @PathVariable String previewPublicId,
            @Valid @ParameterObject ShowtimeSchedulePreviewItemQuery query
    ) {
        if (query.getPage() < 0) {
            throw new com.lorafilm.movie.common.exception.BusinessException(com.lorafilm.movie.common.exception.ErrorCode.VALIDATION_ERROR, "Page must not be less than zero");
        }
        if (query.getSize() < 1 || query.getSize() > 100) {
            throw new com.lorafilm.movie.common.exception.BusinessException(com.lorafilm.movie.common.exception.ErrorCode.VALIDATION_ERROR, "Size must be between 1 and 100");
        }
        ShowtimeSchedulePreviewPageResponse response = service.getPreview(previewPublicId, query);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview retrieved successfully", response));
    }

    @Operation(summary = "Generate preview", description = "Generates a new auto schedule preview.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview generated"),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Related entities not found"),
        @ApiResponse(responseCode = "409", description = "Conflict, e.g. idempotency key reused"),
        @ApiResponse(responseCode = "422", description = "Candidate limit exceeded"),
        @ApiResponse(responseCode = "500", description = "Auto schedule generation failed")
    })
    @PostMapping("/generate-preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<ShowtimeSchedulePreviewSummaryResponse>> generatePreview(
            @Valid @RequestBody com.lorafilm.movie.autoschedule.dto.request.GenerateShowtimeSchedulePreviewRequest request
    ) {
        Long adminUserId = currentUserProvider.getCurrentUserId();
        if (adminUserId == null) {
            throw new com.lorafilm.movie.common.exception.BusinessException(com.lorafilm.movie.common.exception.ErrorCode.CURRENT_USER_NOT_AVAILABLE);
        }
        ShowtimeSchedulePreviewSummaryResponse response = generationService.generatePreview(request, adminUserId);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview generated successfully", response));
    }

    @Operation(
        summary = "Update preview item selections",
        description = "Atomically updates a partial set of candidate selections. The complete final selected set must have non-overlapping half-open occupancy intervals within each auditorium."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Selections updated"),
        @ApiResponse(responseCode = "400", description = "Invalid selection request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Preview or item not found"),
        @ApiResponse(responseCode = "409", description = "Expired, non-editable, stale version, invalid item state, or final-set occupancy overlap")
    })
    @PutMapping("/{previewPublicId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<ShowtimeSchedulePreviewSummaryResponse>> updateSelections(
            @PathVariable String previewPublicId,
            @Valid @RequestBody UpdatePreviewItemSelectionsRequest request
    ) {
        ShowtimeSchedulePreviewSummaryResponse response = service.updateSelections(previewPublicId, request);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview selections updated successfully", response));
    }

    @Operation(
        summary = "Check pricing readiness",
        description = "Read-only pricing preflight for the currently selected preview candidates. No showtimes are created."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pricing readiness checked"),
        @ApiResponse(responseCode = "400", description = "Invalid request or no selected items"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Preview not found"),
        @ApiResponse(responseCode = "409", description = "Expired, stale, or non-applicable preview")
    })
    @PostMapping("/{previewPublicId}/pricing-readiness")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<AutoSchedulePricingPreflightResponse>> checkPricingReadiness(
            @PathVariable String previewPublicId,
            @Valid @RequestBody CheckAutoSchedulePricingRequest request
    ) {
        AutoSchedulePricingPreflightResponse response = pricingReadinessService.check(
                previewPublicId, request.getExpectedVersion());
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok(
                "Auto schedule pricing readiness checked successfully", response));
    }

    @Operation(summary = "Cancel an un-applied auto schedule preview", description = "Discards a PREVIEWED schedule without creating or changing any showtimes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview cancelled"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Preview not found"),
        @ApiResponse(responseCode = "409", description = "Expired, stale version, or non-cancellable state")
    })
    @PostMapping("/{previewPublicId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<ShowtimeSchedulePreviewSummaryResponse>> cancelPreview(
            @PathVariable String previewPublicId,
            @Valid @RequestBody CancelShowtimeSchedulePreviewRequest request
    ) {
        ShowtimeSchedulePreviewSummaryResponse response = service.cancelPreview(previewPublicId, request);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview cancelled successfully", response));
    }

    @Operation(summary = "Apply an auto schedule preview", description = "Atomically revalidate selected preview candidates and create real showtimes using ALL_OR_NOTHING semantics.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Applied or idempotently replayed"),
        @ApiResponse(responseCode = "400", description = "Invalid request or no selected items"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Preview not found"),
        @ApiResponse(responseCode = "409", description = "Expired, stale version, invalid state, conflict, idempotency conflict"),
        @ApiResponse(responseCode = "500", description = "Unexpected apply failure")
    })
    @PostMapping("/{previewPublicId}/apply")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse>> applyPreview(
            @PathVariable String previewPublicId,
            @Valid @RequestBody com.lorafilm.movie.autoschedule.dto.request.ApplyShowtimeSchedulePreviewRequest request
    ) {
        com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse response = applyService.applyPreview(previewPublicId, request);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview applied successfully", response));
    }

    @Operation(summary = "Get eligible movies", description = "Retrieves the list of eligible movies for auto schedule configuration.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Movies retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/eligible-movies")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<java.util.List<com.lorafilm.movie.autoschedule.dto.response.EligibleMovieResponse>>> getEligibleMovies(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate
    ) {
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok(eligibilityService.getEligibleMovies(fromDate, toDate)));
    }
}
