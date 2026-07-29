package com.project.authservice.controller;

import com.project.authservice.common.ApiResponse;
import com.project.authservice.dto.AccountDto;
import com.project.authservice.enums.AccountStatus;
import com.project.authservice.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasAnyAuthority('SYSTEM_CONFIGURATION', 'EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<Page<AccountDto>>> getAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Long roleId,
            Pageable pageable) {
        log.info("Get accounts called");
        Page<AccountDto> accounts = accountService.getAccounts(keyword, status, roleId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Success", accounts));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountById(@PathVariable Long id) {
        log.info("Get account by id called: {}", id);
        AccountDto account = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success("Success", account));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<AccountDto>> updateStatus(@PathVariable Long id, @RequestParam AccountStatus status) {
        log.info("Update account status called: id={}, status={}", id, status);
        AccountDto account = accountService.updateAccountStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", account));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('SYSTEM_CONFIGURATION')")
    public ResponseEntity<ApiResponse<AccountDto>> updateRole(@PathVariable Long id, @RequestParam Long roleId) {
        log.info("Update account role called: id={}, roleId={}", id, roleId);
        AccountDto account = accountService.updateAccountRole(id, roleId);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", account));
    }

    @PostMapping("/employee")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<AccountDto>> createEmployeeAccount(@jakarta.validation.Valid @RequestBody com.project.authservice.dto.request.EmployeeAccountRequest request) {
        log.info("Create employee account called: email={}", request.getEmail());
        AccountDto account = accountService.createEmployeeAccount(request);
        return ResponseEntity.status(201).body(ApiResponse.success("Employee account created successfully", account));
    }
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
}
