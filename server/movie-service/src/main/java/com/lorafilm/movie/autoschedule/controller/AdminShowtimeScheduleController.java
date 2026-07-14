package com.lorafilm.movie.autoschedule.controller;

import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/showtime-schedules")
@Tag(name = "Admin Auto Showtime Scheduling API", description = "Admin endpoints for reviewing persisted auto scheduling previews")
public class AdminShowtimeScheduleController {

    private final ShowtimeSchedulePreviewService service;

    public AdminShowtimeScheduleController(ShowtimeSchedulePreviewService service) {
        this.service = service;
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
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<ShowtimeSchedulePreviewResponse>> getPreview(
            @PathVariable String previewPublicId
    ) {
        ShowtimeSchedulePreviewResponse response = service.getPreview(previewPublicId);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview retrieved successfully", response));
    }

    @Operation(summary = "Update preview item selections", description = "Update the selection status of candidate items in a preview.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Selections updated"),
        @ApiResponse(responseCode = "400", description = "Invalid selection request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Preview or item not found"),
        @ApiResponse(responseCode = "409", description = "Expired, non-editable, or version conflict")
    })
    @PutMapping("/{previewPublicId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.lorafilm.movie.common.api.ApiResponse<ShowtimeSchedulePreviewResponse>> updateSelections(
            @PathVariable String previewPublicId,
            @Valid @RequestBody UpdatePreviewItemSelectionsRequest request
    ) {
        ShowtimeSchedulePreviewResponse response = service.updateSelections(previewPublicId, request);
        return ResponseEntity.ok(com.lorafilm.movie.common.api.ApiResponse.ok("Auto schedule preview selections updated successfully", response));
    }
}
