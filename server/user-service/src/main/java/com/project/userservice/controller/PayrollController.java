package com.project.userservice.controller;

import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.security.CurrentActor;
import com.project.userservice.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/payrolls")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Payroll")
public class PayrollController {
    private final PayrollService service;

    public PayrollController(PayrollService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Page<PayrollResponse>>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) PayrollStatus status,
            @RequestParam(required = false) String month,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payrolls retrieved",
                service.search(employeeId, status, month, pageable)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<Page<PayrollResponse>>> mine(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payrolls retrieved",
                service.search(CurrentActor.accountId(), null, null, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PayrollResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payroll retrieved", service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PayrollResponse>> create(@Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Payroll created", service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PayrollResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll updated", service.update(id, request)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PayrollResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payroll approved", service.approve(id)));
    }

    @PutMapping("/{id}/paid")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PayrollResponse>> paid(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payroll marked as paid", service.markPaid(id)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<PayrollResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payroll cancelled", service.cancel(id)));
    }
}
