package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.CustomerResponse;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/customers")
@Tag(name = "Customers")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Search customers")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved", service.search(keyword, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", service.get(id)));
    }

    @PutMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> block(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer blocked", service.changeStatus(id, UserStatus.BLOCKED)));
    }

    @PutMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerResponse>> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer unblocked", service.changeStatus(id, UserStatus.ACTIVE)));
    }
}
