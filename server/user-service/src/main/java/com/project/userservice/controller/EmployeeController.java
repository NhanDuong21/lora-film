package com.project.userservice.controller;

import com.project.userservice.dto.request.EmployeeRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.EmployeeResponse;
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
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Employees retrieved",
                service.search(keyword, status, departmentId, positionId, pageable)));
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> get(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee retrieved", service.get(accountId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Employee created", service.create(request)));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable Long accountId,
                                                                 @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated", service.update(accountId, request)));
    }

    @PutMapping("/{accountId}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> suspend(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee suspended",
                service.changeStatus(accountId, EmployeeStatus.SUSPENDED)));
    }

    @PutMapping("/{accountId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activate(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee activated",
                service.changeStatus(accountId, EmployeeStatus.ACTIVE)));
    }

    @PutMapping("/{accountId}/resign")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> resign(@PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Employee resigned",
                service.changeStatus(accountId, EmployeeStatus.RESIGNED)));
    }

    @PutMapping("/{accountId}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_ASSIGN_POSITION')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> transfer(@PathVariable Long accountId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId) {
        return ResponseEntity.ok(ApiResponse.success("Employee transferred",
                service.transfer(accountId, departmentId, positionId)));
    }
}
