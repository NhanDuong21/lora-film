package com.lorafilm.movie.autoschedule.controller;

import com.lorafilm.movie.autoschedule.dto.request.UpdatePreviewItemSelectionsRequest;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.service.ShowtimeSchedulePreviewService;
import com.lorafilm.movie.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    @GetMapping("/{previewPublicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimeSchedulePreviewResponse>> getPreview(
            @PathVariable String previewPublicId
    ) {
        ShowtimeSchedulePreviewResponse response = service.getPreview(previewPublicId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "Update preview item selections", description = "Update the selection status of candidate items in a preview.")
    @PutMapping("/{previewPublicId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShowtimeSchedulePreviewResponse>> updateSelections(
            @PathVariable String previewPublicId,
            @Valid @RequestBody UpdatePreviewItemSelectionsRequest request
    ) {
        ShowtimeSchedulePreviewResponse response = service.updateSelections(previewPublicId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
