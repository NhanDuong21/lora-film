package com.project.promotionservice.benefit.controller;

import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponCreateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponGenerateRequest;
import com.project.promotionservice.benefit.dto.request.CouponRequests.CouponUpdateRequest;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponImportResult;
import com.project.promotionservice.benefit.dto.response.BenefitResponses.CouponResponse;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.benefit.service.CouponService;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.response.PagedResponse;
import com.project.promotionservice.configuration.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Coupons", description = "Coupon creation, generation, import, export and lifecycle")
public class AdminCouponController {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "updatedAt", "code", "name", "status", "validFrom", "validTo", "priority");

    private final CouponService couponService;

    public AdminCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @Operation(summary = "Create coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> create(
            @Valid @RequestBody CouponCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        CouponResponse data = couponService.create(request, BenefitControllerSupport.actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully", data));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate a unique coupon batch")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> generate(
            @Valid @RequestBody CouponGenerateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<CouponResponse> data = couponService.generate(
                request, BenefitControllerSupport.actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupons generated successfully", data));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import coupons from CSV")
    public ResponseEntity<ApiResponse<CouponImportResult>> importCoupons(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        CouponImportResult data = couponService.importCsv(
                file, BenefitControllerSupport.actor(principal));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon import completed", data));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export coupons as CSV")
    public ResponseEntity<byte[]> exportCoupons(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String campaignPublicId,
            @RequestParam(required = false) CouponStatus status) {
        byte[] data = couponService.exportCsv(keyword, campaignPublicId, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"coupons.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> update(
            @PathVariable("id") String publicId,
            @Valid @RequestBody CouponUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon updated successfully",
                couponService.update(publicId, request, BenefitControllerSupport.actor(principal))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Disable coupon")
    public ResponseEntity<ApiResponse<Void>> disable(
            @PathVariable("id") String publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        couponService.disable(publicId, BenefitControllerSupport.actor(principal));
        return ResponseEntity.ok(ApiResponse.success("Coupon disabled successfully", null));
    }

    @GetMapping
    @Operation(summary = "Search coupons")
    public ResponseEntity<ApiResponse<PagedResponse<CouponResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String campaignPublicId,
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) Instant validAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        PagedResponse<CouponResponse> data = couponService.search(
                keyword, campaignPublicId, status, validAt,
                BenefitControllerSupport.pageable(page, size, sort, SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon detail")
    public ResponseEntity<ApiResponse<CouponResponse>> get(@PathVariable("id") String publicId) {
        return ResponseEntity.ok(ApiResponse.success(couponService.get(publicId)));
    }
}
