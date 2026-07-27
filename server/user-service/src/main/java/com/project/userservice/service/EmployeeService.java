package com.project.userservice.service;

import com.project.userservice.dto.request.EmployeeRequest;
import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PositionRepository;
import com.project.userservice.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;

    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository,
                           DepartmentRepository departmentRepository, PositionRepository positionRepository,
                           UserAuditService auditService, UserDomainEventService eventService) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(String keyword, EmployeeStatus status, Long departmentId,
                                         Long positionId, Pageable pageable) {
        Page<Employee> page = employeeRepository.search(keyword, status, departmentId, positionId, pageable);
        Map<Long, User> users = userRepository.findAllById(
                        page.getContent().stream().map(Employee::getAccountId).toList())
                .stream().collect(Collectors.toMap(User::getAccountId, Function.identity()));
        return page.map(employee -> map(employee, users.get(employee.getAccountId())));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long accountId) {
        Employee employee = find(accountId);
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        return map(employee, user);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsById(request.accountId())) {
            throw new BusinessException("Employee already exists", "USER_EMPLOYEE_DUPLICATE");
        }
        User user = userRepository.findById(request.accountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account must be active", "USER_ACCOUNT_INACTIVE");
        }
        Employee employee = new Employee();
        employee.setAccountId(request.accountId());
        employee.setEmployeeCode("EMP" + String.format("%08d", request.accountId()));
        apply(employee, request);
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(employee);
        auditService.log("EMPLOYEE_CREATED", "EMPLOYEE", employee.getAccountId(), null);
        eventService.record("EMPLOYEE_CREATED", "EMPLOYEE", employee.getAccountId(),
                eventData(employee));
        return map(employee, user);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public EmployeeResponse update(Long accountId, EmployeeRequest request) {
        Employee employee = find(accountId);
        if (!accountId.equals(request.accountId())) {
            throw new BusinessException("Employee account cannot be changed", "USER_010");
        }
        apply(employee, request);
        employeeRepository.save(employee);
        auditService.log("EMPLOYEE_UPDATED", "EMPLOYEE", accountId, null);
        eventService.record("EMPLOYEE_UPDATED", "EMPLOYEE", accountId, eventData(employee));
        return get(accountId);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public EmployeeResponse changeStatus(Long accountId, EmployeeStatus status) {
        Employee employee = find(accountId);
        if (employee.getStatus() == EmployeeStatus.RESIGNED && status != EmployeeStatus.RESIGNED) {
            throw new BusinessException("A resigned employee cannot be reactivated", "USER_010");
        }
        if (employee.getStatus() == status) {
            return get(accountId);
        }
        employee.setStatus(status);
        employeeRepository.save(employee);
        String eventType = status == EmployeeStatus.RESIGNED ? "EMPLOYEE_RESIGNED" : "EMPLOYEE_UPDATED";
        auditService.log(eventType, "EMPLOYEE", accountId, "status=" + status);
        eventService.record(eventType, "EMPLOYEE", accountId, eventData(employee));
        return get(accountId);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public EmployeeResponse transfer(Long accountId, Long departmentId, Long positionId) {
        if (departmentId == null && positionId == null) {
            throw new BusinessException("Department or position is required", "USER_008");
        }
        Employee employee = find(accountId);
        if (departmentId != null) {
            employee.setDepartment(findDepartment(departmentId));
        }
        if (positionId != null) {
            employee.setPosition(findPosition(positionId));
        }
        employeeRepository.save(employee);
        auditService.log("EMPLOYEE_TRANSFERRED", "EMPLOYEE", accountId, null);
        eventService.record("EMPLOYEE_TRANSFERRED", "EMPLOYEE", accountId, eventData(employee));
        return get(accountId);
    }

    private void apply(Employee employee, EmployeeRequest request) {
        employee.setDepartment(findDepartment(request.departmentId()));
        employee.setPosition(findPosition(request.positionId()));
        employee.setHireDate(request.hireDate());
        employee.setBaseSalary(request.baseSalary());
    }

    private Department findDepartment(Long id) {
        Department value = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Department not found", "USER_004"));
        if (value.isDeleted()) {
            throw new BusinessException("Department not found", "USER_004");
        }
        return value;
    }

    private Position findPosition(Long id) {
        Position value = positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Position not found", "USER_005"));
        if (value.isDeleted()) {
            throw new BusinessException("Position not found", "USER_005");
        }
        return value;
    }

    private Employee find(Long accountId) {
        Employee employee = employeeRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Employee not found", "USER_003"));
        if (employee.isDeleted()) {
            throw new BusinessException("Employee not found", "USER_003");
        }
        return employee;
    }

    private Map<String, Object> eventData(Employee employee) {
        return Map.of(
                "employeeId", employee.getAccountId(),
                "employeeCode", employee.getEmployeeCode(),
                "departmentId", employee.getDepartment().getId(),
                "positionId", employee.getPosition().getId(),
                "status", employee.getStatus().name());
    }

    private EmployeeResponse map(Employee employee, User user) {
        if (user == null) {
            throw new BusinessException("User not found", "USER_001");
        }
        return new EmployeeResponse(employee.getAccountId(), employee.getEmployeeCode(),
                user.getFullName(), user.getPhoneNumber(),
                employee.getDepartment().getId(), employee.getDepartment().getCode(),
                employee.getDepartment().getName(), employee.getPosition().getId(),
                employee.getPosition().getCode(), employee.getPosition().getTitle(),
                employee.getHireDate(), employee.getBaseSalary(), employee.getStatus());
    }
}
