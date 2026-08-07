package com.project.userservice.controller;

import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.dto.request.PayrollActionRequest;
import com.project.userservice.dto.request.PayrollGenerationRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.dto.response.PayrollSummaryResponse;
import com.project.userservice.dto.response.PayrollGenerationResponse;
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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('PAYROLL_VIEW')")
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
    public ResponseEntity<ApiResponse<Page<PayrollResponse>>> mine(
            @RequestParam(required = false) String month,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payrolls retrieved",
                service.searchMine(CurrentActor.accountId(), month, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<PayrollResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payroll retrieved", service.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PAYROLL_CREATE')")
    public ResponseEntity<ApiResponse<PayrollResponse>> create(@Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Payroll created", service.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PAYROLL_UPDATE')")
    public ResponseEntity<ApiResponse<PayrollResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll updated", service.update(id, request)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<PayrollSummaryResponse>> summary(@RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.success("Payroll summary retrieved", service.summary(month)));
    }

    @PostMapping("/{id}/actions")
    @PreAuthorize("hasRole('ADMIN') or (#request.type != null and ("
            + "(#request.type.name() == 'APPROVE' and hasAuthority('PAYROLL_APPROVE')) or "
            + "(#request.type.name() != 'APPROVE' and hasAuthority('PAYROLL_UPDATE'))))")
    public ResponseEntity<ApiResponse<PayrollResponse>> action(@PathVariable Long id,
            @Valid @RequestBody PayrollActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll action recorded",
                service.applyAction(id, request)));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('PAYROLL_CREATE')")
    public ResponseEntity<ApiResponse<PayrollGenerationResponse>> generate(
            @Valid @RequestBody PayrollGenerationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payroll generated from timekeeping",
                service.generateFromTimekeeping(request)));
    }

}
