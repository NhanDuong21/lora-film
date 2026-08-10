package com.lorafilm.movie.auditorium.controller;

import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.CloneAuditoriumRequest;

import com.lorafilm.movie.auditorium.service.AuditoriumService;
import com.lorafilm.movie.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditoriumController {

    private final AuditoriumService auditoriumService;

    public AdminAuditoriumController(AuditoriumService auditoriumService) {
        this.auditoriumService = auditoriumService;
    }

    @Operation(summary = "Create an auditorium")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidEnumErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.ConflictErrorResponse.class)))
    })
    @PostMapping("/cinemas/{cinemaPublicId}/auditoriums")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> createAuditorium(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody CreateAuditoriumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(auditoriumService.createAuditorium(cinemaPublicId, request)));
    }

    @Operation(summary = "Update an auditorium")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(oneOf = {com.lorafilm.movie.common.api.error.ValidationErrorResponse.class, com.lorafilm.movie.common.api.error.InvalidEnumErrorResponse.class}))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.ConflictErrorResponse.class)))
    })
    @PutMapping("/auditoriums/{auditoriumPublicId}")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> updateAuditorium(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody UpdateAuditoriumRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(auditoriumService.updateAuditorium(auditoriumPublicId, request)));
    }

    @Operation(summary = "Clone an auditorium layout")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found")
    })
    @PostMapping("/cinemas/{cinemaPublicId}/auditoriums/{auditoriumPublicId}/clone")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> cloneAuditoriumLayout(
            @PathVariable String cinemaPublicId,
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody CloneAuditoriumRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(auditoriumService.cloneAuditoriumLayout(cinemaPublicId, auditoriumPublicId, request)));
    }

    @Operation(summary = "Delete an auditorium")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.NotFoundErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = com.lorafilm.movie.common.api.error.ConflictErrorResponse.class)))
    })
    @DeleteMapping("/auditoriums/{auditoriumPublicId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuditorium(@PathVariable String auditoriumPublicId) {
        auditoriumService.deleteAuditorium(auditoriumPublicId);
        return ResponseEntity.ok(ApiResponse.ok("Auditorium deleted successfully"));
    }
}
