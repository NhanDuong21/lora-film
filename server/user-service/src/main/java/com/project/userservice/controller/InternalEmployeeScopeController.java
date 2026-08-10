package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.EmployeeCinemaScopeResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.repository.EmployeeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/employees")
public class InternalEmployeeScopeController {
    private final EmployeeRepository employeeRepository;

    public InternalEmployeeScopeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/{accountId}/cinema-scope")
    public ResponseEntity<ApiResponse<EmployeeCinemaScopeResponse>> cinemaScope(
            @PathVariable Long accountId) {
        Employee employee = employeeRepository.findByAccountIdAndIsDeletedFalse(accountId)
                .orElse(null);
        EmployeeCinemaScopeResponse response = employee == null
                ? new EmployeeCinemaScopeResponse(accountId, null, "NOT_FOUND")
                : new EmployeeCinemaScopeResponse(
                        accountId, employee.getCinemaPublicId(), employee.getStatus().name());
        return ResponseEntity.ok(ApiResponse.success("Employee cinema scope", response));
    }
}
