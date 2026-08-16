package com.project.userservice.controller;

import com.project.userservice.dto.request.EmployeeDirectoryRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.EmployeeCinemaScopeResponse;
import com.project.userservice.dto.response.EmployeeDirectoryResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/employees")
public class InternalEmployeeScopeController {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public InternalEmployeeScopeController(
            EmployeeRepository employeeRepository,
            UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
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

    @PostMapping("/directory")
    public ResponseEntity<ApiResponse<List<EmployeeDirectoryResponse>>> directory(
            @Valid @RequestBody EmployeeDirectoryRequest request) {
        List<Long> accountIds = request.accountIds().stream().distinct().toList();
        Map<Long, User> users = userRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(User::getAccountId, Function.identity()));
        Map<Long, Employee> employees = employeeRepository.findByAccountIdIn(accountIds).stream()
                .collect(Collectors.toMap(Employee::getAccountId, Function.identity()));

        Map<Long, EmployeeDirectoryResponse> directory = new LinkedHashMap<>();
        for (Long accountId : accountIds) {
            User user = users.get(accountId);
            if (user == null) continue;
            Employee employee = employees.get(accountId);
            Position position = employee == null ? null : employee.getPosition();
            directory.put(accountId, new EmployeeDirectoryResponse(
                    accountId,
                    employee == null ? null : employee.getEmployeeCode(),
                    user.getFullName(),
                    user.getAvatarUrl(),
                    position == null ? null : position.getCode(),
                    position == null ? null : position.getTitle()));
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Employee directory loaded", List.copyOf(directory.values())));
    }
}
