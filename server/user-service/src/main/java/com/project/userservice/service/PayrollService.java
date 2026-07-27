package com.project.userservice.service;

import com.project.userservice.dto.request.PayrollDetailRequest;
import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.dto.response.PayrollDetailResponse;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.Payroll;
import com.project.userservice.entity.PayrollDetail;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.PayrollDetailType;
import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PayrollRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.security.CurrentActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;

    public PayrollService(PayrollRepository payrollRepository, EmployeeRepository employeeRepository,
                          UserRepository userRepository, UserAuditService auditService,
                          UserDomainEventService eventService) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<PayrollResponse> search(Long employeeId, PayrollStatus status, String month, Pageable pageable) {
        LocalDate salaryMonth = month == null || month.isBlank() ? null : parseMonth(month);
        Page<Payroll> page = payrollRepository.search(employeeId, status, salaryMonth, pageable);
        Map<Long, User> users = userRepository.findAllById(page.getContent().stream()
                        .map(p -> p.getEmployee().getAccountId()).distinct().toList())
                .stream().collect(Collectors.toMap(User::getAccountId, Function.identity()));
        return page.map(p -> map(p, users.get(p.getEmployee().getAccountId()), false));
    }

    @Transactional(readOnly = true)
    public PayrollResponse get(Long id) {
        Payroll payroll = payrollRepository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException("Payroll not found", "USER_006"));
        User user = userRepository.findById(payroll.getEmployee().getAccountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        return map(payroll, user, true);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse create(PayrollRequest request) {
        LocalDate month = parseMonth(request.salaryMonth());
        if (payrollRepository.existsByEmployeeAccountIdAndSalaryMonth(request.employeeId(), month)) {
            throw new BusinessException("Payroll already exists for employee and month", "USER_PAYROLL_DUPLICATE");
        }
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BusinessException("Employee not found", "USER_003"));
        if (employee.isDeleted() || employee.getStatus() == EmployeeStatus.RESIGNED) {
            throw new BusinessException("Payroll requires an active employee record", "USER_010");
        }
        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setSalaryMonth(month);
        applyAmounts(payroll, request);
        payroll.setStatus(PayrollStatus.PENDING_APPROVAL);
        replaceDetails(payroll, request);
        payroll = payrollRepository.save(payroll);
        auditService.log("PAYROLL_CREATED", "PAYROLL", payroll.getId(), null);
        eventService.record("PAYROLL_CREATED", "PAYROLL", payroll.getId(), eventData(payroll));
        return get(payroll.getId());
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse update(Long id, PayrollRequest request) {
        Payroll payroll = find(id);
        if (payroll.getStatus() == PayrollStatus.APPROVED || payroll.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException("Approved or paid payroll cannot be changed", "USER_010");
        }
        if (!payroll.getEmployee().getAccountId().equals(request.employeeId())
                || !payroll.getSalaryMonth().equals(parseMonth(request.salaryMonth()))) {
            throw new BusinessException("Employee and salary month cannot be changed", "USER_010");
        }
        applyAmounts(payroll, request);
        replaceDetails(payroll, request);
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_UPDATED", "PAYROLL", id, null);
        return get(id);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse approve(Long id) {
        Payroll payroll = find(id);
        if (payroll.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending payroll can be approved", "USER_010");
        }
        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setApprovedBy(CurrentActor.accountId());
        payroll.setApprovedAt(LocalDateTime.now());
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_APPROVED", "PAYROLL", id, null);
        eventService.record("PAYROLL_APPROVED", "PAYROLL", id, eventData(payroll));
        return get(id);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse markPaid(Long id) {
        Payroll payroll = find(id);
        if (payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new BusinessException("Only approved payroll can be paid", "USER_010");
        }
        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaidAt(LocalDateTime.now());
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_PAID", "PAYROLL", id, null);
        eventService.record("PAYROLL_PAID", "PAYROLL", id, eventData(payroll));
        return get(id);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse cancel(Long id) {
        Payroll payroll = find(id);
        if (payroll.getStatus() == PayrollStatus.APPROVED
                || payroll.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException("Approved or paid payroll cannot be cancelled", "USER_010");
        }
        if (payroll.getStatus() == PayrollStatus.CANCELLED) {
            return get(id);
        }
        payroll.setStatus(PayrollStatus.CANCELLED);
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_CANCELLED", "PAYROLL", id, null);
        return get(id);
    }

    private Payroll find(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Payroll not found", "USER_006"));
    }

    private LocalDate parseMonth(String value) {
        try {
            return YearMonth.parse(value).atDay(1);
        } catch (RuntimeException exception) {
            throw new BusinessException("Salary month must use YYYY-MM", "USER_008");
        }
    }

    private void applyAmounts(Payroll payroll, PayrollRequest request) {
        validateDetailTotals(request);
        BigDecimal total = request.basicSalary().add(request.allowance()).add(request.bonus())
                .subtract(request.deduction());
        if (total.signum() < 0) {
            throw new BusinessException("Total salary cannot be negative", "USER_008");
        }
        payroll.setBasicSalary(request.basicSalary());
        payroll.setAllowance(request.allowance());
        payroll.setBonus(request.bonus());
        payroll.setDeduction(request.deduction());
        payroll.setTotalSalary(total);
    }

    private void validateDetailTotals(PayrollRequest request) {
        if (request.details() == null || request.details().isEmpty()) {
            return;
        }
        Map<PayrollDetailType, BigDecimal> totals = request.details().stream().collect(Collectors.groupingBy(
                PayrollDetailRequest::type,
                Collectors.reducing(BigDecimal.ZERO, PayrollDetailRequest::amount, BigDecimal::add)));
        verifyTotal(totals, PayrollDetailType.ALLOWANCE, request.allowance());
        verifyTotal(totals, PayrollDetailType.BONUS, request.bonus());
        verifyTotal(totals, PayrollDetailType.DEDUCTION, request.deduction());
    }

    private void verifyTotal(Map<PayrollDetailType, BigDecimal> totals, PayrollDetailType type, BigDecimal expected) {
        if (totals.containsKey(type) && totals.get(type).compareTo(expected) != 0) {
            throw new BusinessException(type + " detail total does not match payroll summary", "USER_008");
        }
    }

    private void replaceDetails(Payroll payroll, PayrollRequest request) {
        payroll.getDetails().clear();
        if (request.details() == null) {
            return;
        }
        for (PayrollDetailRequest item : request.details()) {
            PayrollDetail detail = new PayrollDetail();
            detail.setPayroll(payroll);
            detail.setType(item.type());
            detail.setDescription(item.description());
            detail.setAmount(item.amount());
            payroll.getDetails().add(detail);
        }
    }

    private Map<String, Object> eventData(Payroll payroll) {
        return Map.of("payrollId", payroll.getId(), "employeeId", payroll.getEmployee().getAccountId(),
                "salaryMonth", payroll.getSalaryMonth().toString(), "totalSalary", payroll.getTotalSalary(),
                "status", payroll.getStatus().name());
    }

    private PayrollResponse map(Payroll payroll, User user, boolean includeDetails) {
        return new PayrollResponse(payroll.getId(), payroll.getEmployee().getAccountId(),
                payroll.getEmployee().getEmployeeCode(), user == null ? null : user.getFullName(),
                payroll.getSalaryMonth(), payroll.getBasicSalary(), payroll.getAllowance(), payroll.getBonus(),
                payroll.getDeduction(), payroll.getTotalSalary(), payroll.getStatus(), payroll.getApprovedBy(),
                payroll.getApprovedAt(), payroll.getPaidAt(),
                includeDetails ? payroll.getDetails().stream()
                        .map(d -> new PayrollDetailResponse(d.getId(), d.getType(), d.getDescription(), d.getAmount()))
                        .toList() : Collections.emptyList());
    }
}
