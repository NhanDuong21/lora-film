package com.lorafilm.movie.auditorium.controller;

import com.lorafilm.movie.auditorium.dto.AuditoriumResponse;
import com.lorafilm.movie.auditorium.dto.CreateAuditoriumRequest;
import com.lorafilm.movie.auditorium.dto.UpdateAuditoriumRequest;

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
@ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not Found"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
})
public class AdminAuditoriumController {

    private final AuditoriumService auditoriumService;

    public AdminAuditoriumController(AuditoriumService auditoriumService) {
        this.auditoriumService = auditoriumService;
    }

    @PostMapping("/cinemas/{cinemaPublicId}/auditoriums")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> createAuditorium(
            @PathVariable String cinemaPublicId,
            @Valid @RequestBody CreateAuditoriumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(auditoriumService.createAuditorium(cinemaPublicId, request)));
    }

    @PutMapping("/auditoriums/{auditoriumPublicId}")
    public ResponseEntity<ApiResponse<AuditoriumResponse>> updateAuditorium(
            @PathVariable String auditoriumPublicId,
            @Valid @RequestBody UpdateAuditoriumRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(auditoriumService.updateAuditorium(auditoriumPublicId, request)));
    }



    @DeleteMapping("/auditoriums/{auditoriumPublicId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuditorium(@PathVariable String auditoriumPublicId) {
        auditoriumService.deleteAuditorium(auditoriumPublicId);
        return ResponseEntity.ok(ApiResponse.ok("Auditorium deleted successfully"));
    }
}
