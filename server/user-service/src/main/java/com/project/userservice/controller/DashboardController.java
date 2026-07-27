package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.DashboardSummaryResponse;
import com.project.userservice.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/dashboard")
@io.swagger.v3.oas.annotations.tags.Tag(name = "User dashboard")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved", service.summary()));
    }

    @GetMapping({"/customers", "/employees", "/payrolls"})
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> domainSummary() {
        return summary();
    }
}
