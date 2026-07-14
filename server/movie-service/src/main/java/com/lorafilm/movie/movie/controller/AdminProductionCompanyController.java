package com.lorafilm.movie.movie.controller;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.dto.ProductionCompanyDto;
import com.lorafilm.movie.movie.dto.ProductionCompanyRequest;
import com.lorafilm.movie.movie.service.AdminProductionCompanyService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/production-companies")
@Validated
public class AdminProductionCompanyController {

    private final AdminProductionCompanyService adminProductionCompanyService;

    public AdminProductionCompanyController(AdminProductionCompanyService adminProductionCompanyService) {
        this.adminProductionCompanyService = adminProductionCompanyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ProductionCompanyDto> createProductionCompany(@Valid @RequestBody ProductionCompanyRequest request) {
        return ApiResponse.ok(adminProductionCompanyService.createProductionCompany(request));
    }

    @PutMapping("/{companyId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ProductionCompanyDto> updateProductionCompany(@PathVariable("companyId") String companyId, @Valid @RequestBody ProductionCompanyRequest request) {
        return ApiResponse.ok(adminProductionCompanyService.updateProductionCompany(companyId, request));
    }
}
