package com.project.userservice.service;

import com.project.userservice.dto.request.EmployeeRequest;
import com.project.userservice.dto.request.EmploymentActionRequest;
import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.dto.response.EligibleEmployeeAccountResponse;
import com.project.userservice.dto.response.EmploymentActionResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.EmploymentAction;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.EmploymentActionType;
import com.project.userservice.enumtype.UserStatus;
import com.project.userservice.enumtype.AccountType;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.mapper.EmployeeMapper;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.EmploymentActionRepository;
import com.project.userservice.repository.PositionRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.security.CurrentActor;
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
    private final EmploymentActionRepository employmentActionRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository, UserRepository userRepository,
                           DepartmentRepository departmentRepository, PositionRepository positionRepository,
                           EmploymentActionRepository employmentActionRepository,
                           UserAuditService auditService, UserDomainEventService eventService,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.positionRepository = positionRepository;
        this.employmentActionRepository = employmentActionRepository;
        this.auditService = auditService;
        this.eventService = eventService;
        this.employeeMapper = employeeMapper;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(String keyword, EmployeeStatus status, Long departmentId,
                                         Long positionId, Pageable pageable) {
        Page<Employee> page = employeeRepository.search(keyword, status, departmentId, positionId,
                com.project.userservice.util.PageableUtils.sanitize(pageable,
                        java.util.Set.of("accountId", "employeeCode", "hireDate", "baseSalary", "status",
                                "createdAt", "updatedAt"),
                        "createdAt", org.springframework.data.domain.Sort.Direction.DESC));
        Map<Long, User> users = userRepository.findAllById(
                        page.getContent().stream().map(Employee::getAccountId).toList())
                .stream().collect(Collectors.toMap(User::getAccountId, Function.identity()));
        return page.map(employee -> employeeMapper.toResponse(
                employee, users.get(employee.getAccountId())));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long accountId) {
        Employee employee = find(accountId);
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        return employeeMapper.toResponse(employee, user);
    }

    @Transactional(readOnly = true)
    public Page<EligibleEmployeeAccountResponse> eligibleAccounts(String keyword, Pageable pageable) {
        Pageable safe = com.project.userservice.util.PageableUtils.sanitize(pageable,
                java.util.Set.of("accountId", "fullName", "email", "createdAt"),
                "fullName", org.springframework.data.domain.Sort.Direction.ASC);
        return userRepository.findEligibleEmployeeAccounts(
                        keyword == null || keyword.isBlank() ? null : keyword.trim(), safe)
                .map(user -> new EligibleEmployeeAccountResponse(
                        user.getAccountId(), user.getFullName(), user.getEmail(), user.getPhoneNumber()));
    }

    @Transactional(readOnly = true)
    public Page<EmploymentActionResponse> actionHistory(Long accountId, Pageable pageable) {
        find(accountId);
        Pageable safe = com.project.userservice.util.PageableUtils.sanitize(pageable,
                java.util.Set.of("id", "effectiveDate", "createdAt", "actionType"),
                "createdAt", org.springframework.data.domain.Sort.Direction.DESC);
        return employmentActionRepository.findByEmployeeAccountId(accountId, safe)
                .map(this::toActionResponse);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public EmployeeResponse applyAction(Long accountId, EmploymentActionRequest request) {
        Employee employee = find(accountId);
        if (request.expectedVersion() != null && !request.expectedVersion().equals(employee.getVersion())) {
            throw new BusinessException("Employee record was changed by another operator", "USER_VERSION_CONFLICT");
        }

        EmploymentAction action = snapshotAction(employee, request);
        switch (request.type()) {
            case ACTIVATE, END_LEAVE -> changeStatusForAction(employee, EmployeeStatus.ACTIVE);
            case START_LEAVE -> changeStatusForAction(employee, EmployeeStatus.ON_LEAVE);
            case SUSPEND -> changeStatusForAction(employee, EmployeeStatus.SUSPENDED);
            case RESIGN -> changeStatusForAction(employee, EmployeeStatus.RESIGNED);
            case TRANSFER -> applyTransferAction(employee, request);
            case COMPENSATION_CHANGE -> applyCompensationAction(employee, request);
        }

        employeeRepository.save(employee);
        completeActionSnapshot(action, employee);
        employmentActionRepository.save(action);

        String eventType = request.type() == EmploymentActionType.RESIGN
                ? "EMPLOYEE_RESIGNED" : "EMPLOYEE_UPDATED";
        String details = "action=" + request.type()
                + "; effectiveDate=" + request.effectiveDate()
                + "; reason=" + request.reason().trim();
        auditService.log(eventType, "EMPLOYEE", accountId, details);
        eventService.record(eventType, "EMPLOYEE", accountId, eventData(employee));
        return get(accountId);
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
        if (user.getAccountType() != AccountType.WORKFORCE) {
            throw new BusinessException("A customer account cannot be used as a workforce profile",
                    "USER_EMPLOYEE_ACCOUNT_TYPE_INVALID");
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
        return employeeMapper.toResponse(employee, user);
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
        Department department = departmentId == null ? employee.getDepartment() : findDepartment(departmentId);
        Position position = positionId == null ? employee.getPosition() : findPosition(positionId);
        validatePositionDepartment(position, department);
        employee.setDepartment(department);
        employee.setPosition(position);
        employeeRepository.save(employee);
        auditService.log("EMPLOYEE_TRANSFERRED", "EMPLOYEE", accountId, null);
        eventService.record("EMPLOYEE_TRANSFERRED", "EMPLOYEE", accountId, eventData(employee));
        return get(accountId);
    }

    private void apply(Employee employee, EmployeeRequest request) {
        Department department = findDepartment(request.departmentId());
        Position position = findPosition(request.positionId());
        validatePositionDepartment(position, department);
        employee.setDepartment(department);
        employee.setPosition(position);
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

    private void changeStatusForAction(Employee employee, EmployeeStatus status) {
        if (employee.getStatus() == EmployeeStatus.RESIGNED && status != EmployeeStatus.RESIGNED) {
            throw new BusinessException("A resigned employee cannot be reactivated", "USER_010");
        }
        employee.setStatus(status);
    }

    private void applyTransferAction(Employee employee, EmploymentActionRequest request) {
        if (request.departmentId() == null && request.positionId() == null) {
            throw new BusinessException("A transfer requires a department or position", "USER_008");
        }
        Department department = request.departmentId() == null
                ? employee.getDepartment() : findDepartment(request.departmentId());
        Position position = request.positionId() == null
                ? employee.getPosition() : findPosition(request.positionId());
        validatePositionDepartment(position, department);
        employee.setDepartment(department);
        employee.setPosition(position);
    }

    private void validatePositionDepartment(Position position, Department department) {
        if (position.getDepartment() == null
                || !position.getDepartment().getId().equals(department.getId())) {
            throw new BusinessException("Position does not belong to the selected department",
                    "USER_POSITION_DEPARTMENT_MISMATCH");
        }
    }

    private void applyCompensationAction(Employee employee, EmploymentActionRequest request) {
        if (request.baseSalary() == null || request.baseSalary().signum() <= 0) {
            throw new BusinessException("A compensation change requires a positive base salary", "USER_008");
        }
        employee.setBaseSalary(request.baseSalary());
    }

    private EmploymentAction snapshotAction(Employee employee, EmploymentActionRequest request) {
        EmploymentAction action = new EmploymentAction();
        action.setEmployeeAccountId(employee.getAccountId());
        action.setActionType(request.type());
        action.setEffectiveDate(request.effectiveDate());
        action.setReason(request.reason().trim());
        action.setPreviousStatus(employee.getStatus());
        action.setPreviousDepartmentId(idOf(employee.getDepartment()));
        action.setPreviousPositionId(idOf(employee.getPosition()));
        action.setPreviousBaseSalary(employee.getBaseSalary());
        action.setPerformedBy(CurrentActor.accountId());
        return action;
    }

    private void completeActionSnapshot(EmploymentAction action, Employee employee) {
        action.setNewStatus(employee.getStatus());
        action.setNewDepartmentId(idOf(employee.getDepartment()));
        action.setNewPositionId(idOf(employee.getPosition()));
        action.setNewBaseSalary(employee.getBaseSalary());
    }

    private Long idOf(Department department) {
        return department == null ? null : department.getId();
    }

    private Long idOf(Position position) {
        return position == null ? null : position.getId();
    }

    private EmploymentActionResponse toActionResponse(EmploymentAction action) {
        return new EmploymentActionResponse(
                action.getId(), action.getEmployeeAccountId(), action.getActionType(),
                action.getEffectiveDate(), action.getReason(), action.getPreviousStatus(),
                action.getNewStatus(), action.getPreviousDepartmentId(), action.getNewDepartmentId(),
                action.getPreviousPositionId(), action.getNewPositionId(),
                action.getPreviousBaseSalary(), action.getNewBaseSalary(),
                action.getPerformedBy(), action.getCreatedAt());
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
                "accountId", employee.getAccountId(),
                "employeeId", employee.getAccountId(),
                "employeeCode", employee.getEmployeeCode(),
                "departmentId", employee.getDepartment().getId(),
                "positionId", employee.getPosition().getId(),
                "status", employee.getStatus().name());
    }

}
