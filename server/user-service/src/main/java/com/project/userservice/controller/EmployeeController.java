package com.project.userservice.controller;

import com.project.userservice.dto.request.EmployeeRequest;
import com.project.userservice.dto.request.CinemaAssignmentRequest;
import com.project.userservice.dto.request.EmploymentActionRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.dto.response.EligibleEmployeeAccountResponse;
import com.project.userservice.dto.response.EmploymentActionResponse;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/employees")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAnyAuthority('EMPLOYEE_VIEW', 'PAYROLL_VIEW', 'PAYROLL_CREATE', 'PAYROLL_UPDATE')")
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String cinemaPublicId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved",
                service.search(keyword, status, departmentId, positionId, cinemaPublicId, pageable)));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> get(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved", service.get(accountId)));
    }

    @GetMapping("/eligible-accounts")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<Page<EligibleEmployeeAccountResponse>>> eligibleAccounts(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Eligible employee accounts retrieved", service.eligibleAccounts(keyword, pageable)));
    }

    @GetMapping("/{accountId}/actions")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<Page<EmploymentActionResponse>>> actionHistory(
            @PathVariable Long accountId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Employment action history retrieved", service.actionHistory(accountId, pageable)));
    }

    @PostMapping("/{accountId}/actions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> applyAction(
            @PathVariable Long accountId,
            @jakarta.validation.Valid @RequestBody EmploymentActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Employment action applied", service.applyAction(accountId, request)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Employee created", service.create(request)));
    }

    @PutMapping("/{accountId}/cinema-assignment")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> assignCinema(
            @PathVariable Long accountId,
            @Valid @RequestBody CinemaAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật rạp làm việc",
                service.assignCinema(accountId, request.cinemaPublicId())));
    }

}
