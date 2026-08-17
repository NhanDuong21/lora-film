package com.lorafilm.movie.auditorium.controller;

import com.lorafilm.movie.auditorium.dto.AuditoriumLayoutSourcePreview;
import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumAsNewRequest;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumFromTemplateRequest;
import com.lorafilm.movie.auditorium.service.AuditoriumLayoutSourceService;
import com.lorafilm.movie.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditoriumLayoutSourceController {

    private final AuditoriumLayoutSourceService layoutSourceService;

    public AdminAuditoriumLayoutSourceController(AuditoriumLayoutSourceService layoutSourceService) {
        this.layoutSourceService = layoutSourceService;
    }

    @Operation(summary = "List complete system auditorium layout templates")
    @GetMapping("/auditorium-layout-templates")
    public ApiResponse<List<AuditoriumLayoutSourcePreview>> getTemplates() {
        return ApiResponse.ok(layoutSourceService.getSystemTemplates());
    }

    @Operation(summary = "Preview an existing auditorium as a clone source")
    @GetMapping("/auditoriums/{auditoriumPublicId}/clone-preview")
    public ApiResponse<AuditoriumLayoutSourcePreview> getClonePreview(
            @PathVariable String auditoriumPublicId) {
        return ApiResponse.ok(layoutSourceService.getClonePreview(auditoriumPublicId));
    }

    @Operation(summary = "Validate the persisted layout of an auditorium")
    @PostMapping("/auditoriums/{auditoriumPublicId}/validate-layout")
    public ApiResponse<AuditoriumLayoutSourcePreview> validateLayout(
            @PathVariable String auditoriumPublicId) {
        return ApiResponse.ok(layoutSourceService.getClonePreview(auditoriumPublicId));
    }

    @Operation(summary = "Atomically create an auditorium from a complete system template")
    @PostMapping("/auditoriums/from-template")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> createFromTemplate(
            @Valid @RequestBody CreateAuditoriumFromTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(layoutSourceService.createFromTemplate(request)));
    }

    @Operation(summary = "Atomically create a new auditorium by cloning an existing layout")
    @PostMapping("/auditoriums/{sourceAuditoriumPublicId}/clone")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> cloneAsNew(
            @PathVariable String sourceAuditoriumPublicId,
            @Valid @RequestBody CloneAuditoriumAsNewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(layoutSourceService.cloneAsNew(sourceAuditoriumPublicId, request)));
    }
}
