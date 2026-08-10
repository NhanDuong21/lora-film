package com.lorafilm.movie.pricing.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.pricing.dto.request.ActivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.CopyPricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.CreatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.DeactivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.PriceResolutionPreviewRequest;
import com.lorafilm.movie.pricing.dto.request.UpdatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.response.PricePolicyResponse;
import com.lorafilm.movie.pricing.dto.response.PricePolicyUsageResponse;
import com.lorafilm.movie.pricing.dto.response.PriceResolutionPreviewResponse;
import com.lorafilm.movie.pricing.service.PricePolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminPricePolicyController {

    private final PricePolicyService pricePolicyService;

    public AdminPricePolicyController(PricePolicyService pricePolicyService) {
        this.pricePolicyService = pricePolicyService;
    }

    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<PageResponse<PricePolicyResponse>>> search(
            @RequestParam(required = false) String cinema,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate effectiveDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                pricePolicyService.search(cinema, status, effectiveDate, page, size)));
    }

    @PostMapping("/policies")
    public ResponseEntity<ApiResponse<PricePolicyResponse>> create(
            @Valid @RequestBody CreatePricePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(pricePolicyService.create(request)));
    }

    @GetMapping("/policies/{publicId}")
    public ResponseEntity<ApiResponse<PricePolicyResponse>> get(@PathVariable String publicId) {
        return ResponseEntity.ok(ApiResponse.ok(pricePolicyService.get(publicId)));
    }

    @PutMapping("/policies/{publicId}")
    public ResponseEntity<ApiResponse<PricePolicyResponse>> update(
            @PathVariable String publicId,
            @Valid @RequestBody UpdatePricePolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricePolicyService.update(publicId, request)));
    }

    @PostMapping("/policies/{publicId}/activate")
    public ResponseEntity<ApiResponse<PricePolicyResponse>> activate(
            @PathVariable String publicId,
            @Valid @RequestBody ActivatePricePolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricePolicyService.activate(publicId, request)));
    }

    @PostMapping("/policies/{publicId}/deactivate")
    public ResponseEntity<ApiResponse<PricePolicyResponse>> deactivate(
            @PathVariable String publicId,
            @Valid @RequestBody DeactivatePricePolicyRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricePolicyService.deactivate(publicId, request)));
    }

    @PostMapping("/policies/{publicId}/copy")
    public ResponseEntity<ApiResponse<PricePolicyResponse>> copy(
            @PathVariable String publicId,
            @Valid @RequestBody CopyPricePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(pricePolicyService.copy(publicId, request)));
    }

    @GetMapping("/policies/{publicId}/usage")
    public ResponseEntity<ApiResponse<PricePolicyUsageResponse>> usage(
            @PathVariable String publicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(pricePolicyService.usage(publicId, page, size)));
    }

    @PostMapping("/resolve-preview")
    public ResponseEntity<ApiResponse<PriceResolutionPreviewResponse>> preview(
            @RequestBody PriceResolutionPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pricePolicyService.preview(request)));
    }
}
