package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.CustomerResponse;
import com.project.userservice.dto.response.CustomerCounterLookupResponse;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.dto.request.CustomerAccessActionRequest;
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('CUSTOMER_VIEW')")
    @Operation(summary = "Search customers")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved", service.search(keyword, status, pageable)));
    }

    @GetMapping("/counter-search")
    @PreAuthorize("hasAuthority('BOOKING_MANAGE') or hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Search active customers for counter service")
    public ResponseEntity<ApiResponse<Page<CustomerCounterLookupResponse>>> counterSearch(
            @RequestParam String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Counter customers retrieved", service.searchForCounter(keyword, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<ApiResponse<CustomerResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved", service.get(id)));
    }

    @PostMapping("/{id}/access-actions")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CUSTOMER_UPDATE')")
    public ResponseEntity<ApiResponse<CustomerResponse>> applyAccessAction(
            @PathVariable Long id,
            @jakarta.validation.Valid @RequestBody CustomerAccessActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Customer access action applied", service.applyAccessAction(id, request)));
    }

}
