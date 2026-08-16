package com.project.userservice.service;

import com.project.userservice.dto.request.PayrollDetailRequest;
import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.dto.request.PayrollActionRequest;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.dto.response.PayrollSummaryResponse;
import com.project.userservice.dto.response.PayrollGenerationResponse;
import com.project.userservice.dto.request.PayrollGenerationRequest;
import com.project.userservice.entity.AttendanceRecord;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.LeaveRequest;
import com.project.userservice.entity.Payroll;
import com.project.userservice.entity.PayrollDetail;
import com.project.userservice.entity.WorkShift;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.PayrollDetailType;
import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.enumtype.PayrollActionType;
import com.project.userservice.enumtype.PayrollSourceType;
import com.project.userservice.enumtype.ReconciliationStatus;
import com.project.userservice.enumtype.ShiftStatus;
import com.project.userservice.enumtype.LeaveStatus;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.mapper.PayrollMapper;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PayrollRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.WorkShiftRepository;
import com.project.userservice.repository.AttendanceRecordRepository;
import com.project.userservice.repository.LeaveRequestRepository;
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
import java.time.Duration;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PayrollService {
    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final WorkShiftRepository shiftRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;
    private final PayrollMapper payrollMapper;

    public PayrollService(PayrollRepository payrollRepository, EmployeeRepository employeeRepository,
                          UserRepository userRepository, WorkShiftRepository shiftRepository,
                          AttendanceRecordRepository attendanceRepository,
                          LeaveRequestRepository leaveRepository, UserAuditService auditService,
                          UserDomainEventService eventService, PayrollMapper payrollMapper) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.auditService = auditService;
        this.eventService = eventService;
        this.payrollMapper = payrollMapper;
    }

    @Transactional(readOnly = true)
    public Page<PayrollResponse> search(Long employeeId, PayrollStatus status, String month, Pageable pageable) {
        LocalDate salaryMonth = month == null || month.isBlank() ? null : parseMonth(month);
        Page<Payroll> page = payrollRepository.search(employeeId, status, salaryMonth, sanitize(pageable));
        return mapPage(page);
    }

    @Transactional(readOnly = true)
    public Page<PayrollResponse> searchMine(Long employeeId, String month, Pageable pageable) {
        LocalDate salaryMonth = month == null || month.isBlank() ? null : parseMonth(month);
        Page<Payroll> page = payrollRepository.findVisibleToEmployee(employeeId,
                java.util.EnumSet.of(PayrollStatus.APPROVED, PayrollStatus.PAYMENT_PENDING, PayrollStatus.PAID),
                salaryMonth, sanitize(pageable));
        return mapPage(page);
    }

    private Page<PayrollResponse> mapPage(Page<Payroll> page) {
        Map<Long, User> users = userRepository.findAllById(page.getContent().stream()
                        .map(p -> p.getEmployee().getAccountId()).distinct().toList())
                .stream().collect(Collectors.toMap(User::getAccountId, Function.identity()));
        return page.map(payroll -> payrollMapper.toResponse(payroll,
                users.get(payroll.getEmployee().getAccountId()), Map.of(), false));
    }

    @Transactional(readOnly = true)
    public PayrollResponse get(Long id) {
        Payroll payroll = payrollRepository.findDetailedById(id)
                .orElseThrow(() -> new BusinessException("Payroll not found", "USER_006"));
        User user = userRepository.findById(payroll.getEmployee().getAccountId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
        Map<Long, String> actorNames = userRepository.findAllById(actorIds(payroll)).stream()
                .filter(actor -> actor.getFullName() != null && !actor.getFullName().isBlank())
                .collect(Collectors.toMap(User::getAccountId, User::getFullName));
        return payrollMapper.toResponse(payroll, user, actorNames, true);
    }

    private List<Long> actorIds(Payroll payroll) {
        return java.util.stream.Stream.of(payroll.getCreatedBy(), payroll.getApprovedBy(), payroll.getPaidBy(),
                        payroll.getReconciledBy(), payroll.getCancelledBy())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
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
        payroll.setCreatedBy(currentActorOrNull());
        payroll.setSourceType(PayrollSourceType.MANUAL_EXCEPTION);
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
        if (payroll.getStatus() != PayrollStatus.DRAFT
                && payroll.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only draft or pending payroll can be changed", "USER_010");
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
        return approve(id, null, "Legacy approval");
    }

    @Transactional(readOnly = true)
    public PayrollSummaryResponse summary(String month) {
        LocalDate salaryMonth = parseMonth(month);
        return new PayrollSummaryResponse(
                salaryMonth,
                payrollRepository.countBySalaryMonth(salaryMonth),
                payrollRepository.sumNetAmountBySalaryMonth(salaryMonth),
                payrollRepository.countBySalaryMonthAndStatus(salaryMonth, PayrollStatus.PENDING_APPROVAL),
                payrollRepository.countBySalaryMonthAndStatus(salaryMonth, PayrollStatus.APPROVED),
                payrollRepository.countBySalaryMonthAndStatus(salaryMonth, PayrollStatus.PAYMENT_PENDING),
                payrollRepository.countBySalaryMonthAndStatus(salaryMonth, PayrollStatus.PAID),
                payrollRepository.countBySalaryMonthAndStatus(salaryMonth, PayrollStatus.CANCELLED));
    }

    private PayrollResponse approve(Long id, Integer expectedVersion, String reason) {
        Payroll payroll = find(id);
        assertVersion(payroll, expectedVersion);
        if (payroll.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only pending payroll can be approved", "USER_010");
        }
        Long actorId = CurrentActor.accountId();
        if (payroll.getCreatedBy() != null && payroll.getCreatedBy().equals(actorId)) {
            throw new BusinessException("Payroll creator cannot approve the same payroll",
                    "USER_PAYROLL_MAKER_CHECKER");
        }
        int creditedMinutes = integerValue(payroll.getWorkedMinutes())
                + integerValue(payroll.getPaidLeaveMinutes());
        if (integerValue(payroll.getScheduledMinutes()) > 0 && creditedMinutes == 0
                && (reason == null || reason.trim().length() < 10)) {
            throw new BusinessException(
                    "Payroll has no recorded attendance. A review reason of at least 10 characters is required",
                    "USER_PAYROLL_ATTENDANCE_REVIEW_REQUIRED");
        }
        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setApprovedBy(actorId);
        payroll.setApprovedAt(LocalDateTime.now());
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_APPROVED", "PAYROLL", id, reason);
        eventService.record("PAYROLL_APPROVED", "PAYROLL", id, eventData(payroll));
        return get(id);
    }

    private int integerValue(Integer value) {
        return value == null ? 0 : value;
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse markPaid(Long id) {
        throw new BusinessException("Payment reference is required", "USER_PAYROLL_PAYMENT_REFERENCE_REQUIRED");
    }

    private PayrollResponse submitPayment(Long id, Integer expectedVersion, String bankBatchReference, String reason) {
        Payroll payroll = find(id);
        assertVersion(payroll, expectedVersion);
        if (payroll.getStatus() != PayrollStatus.APPROVED) {
            throw new BusinessException("Only approved payroll can be submitted for payment", "USER_010");
        }
        if (bankBatchReference == null || bankBatchReference.isBlank()) {
            throw new BusinessException("Bank batch reference is required", "USER_PAYROLL_BANK_BATCH_REQUIRED");
        }
        payroll.setStatus(PayrollStatus.PAYMENT_PENDING);
        payroll.setBankBatchReference(bankBatchReference.trim());
        payroll.setReconciliationStatus(ReconciliationStatus.PENDING);
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_PAYMENT_SUBMITTED", "PAYROLL", id, reason);
        eventService.record("PAYROLL_PAYMENT_SUBMITTED", "PAYROLL", id, eventData(payroll));
        return get(id);
    }

    private PayrollResponse reconcile(Long id, PayrollActionRequest request) {
        Payroll payroll = find(id);
        assertVersion(payroll, request.expectedVersion());
        if (payroll.getStatus() != PayrollStatus.PAYMENT_PENDING) {
            throw new BusinessException("Only submitted payroll can be reconciled", "USER_010");
        }
        if (request.reconciliationMatched() == null) {
            throw new BusinessException("Reconciliation outcome is required", "USER_PAYROLL_RECONCILIATION_REQUIRED");
        }
        if (request.accountingReference() == null || request.accountingReference().isBlank()) {
            throw new BusinessException("Accounting reference is required", "USER_PAYROLL_ACCOUNTING_REFERENCE_REQUIRED");
        }
        if (request.paymentReference() == null || request.paymentReference().isBlank()) {
            throw new BusinessException("Bank transaction reference is required",
                    "USER_PAYROLL_PAYMENT_REFERENCE_REQUIRED");
        }
        String paymentReference = request.paymentReference().trim();
        if ((payroll.getPaymentReference() == null || !payroll.getPaymentReference().equals(paymentReference))
                && payrollRepository.existsByPaymentReference(paymentReference)) {
            throw new BusinessException("Bank transaction reference is already reconciled",
                    "USER_PAYROLL_PAYMENT_REFERENCE_DUPLICATE");
        }
        if (!request.reconciliationMatched()
                && (request.reason() == null || request.reason().trim().length() < 5)) {
            throw new BusinessException("Mismatch reason must contain at least 5 characters", "USER_008");
        }
        payroll.setPaymentReference(paymentReference);
        payroll.setAccountingReference(request.accountingReference().trim());
        payroll.setReconciledBy(CurrentActor.accountId());
        payroll.setReconciledAt(LocalDateTime.now());
        payroll.setReconciliationNote(normalizeReason(request.reason(),
                request.reconciliationMatched() ? "Bank and accounting records matched" : null));
        if (request.reconciliationMatched()) {
            payroll.setReconciliationStatus(ReconciliationStatus.MATCHED);
            payroll.setStatus(PayrollStatus.PAID);
            payroll.setPaidBy(CurrentActor.accountId());
            payroll.setPaidAt(LocalDateTime.now());
        } else {
            payroll.setReconciliationStatus(ReconciliationStatus.MISMATCH);
        }
        payrollRepository.save(payroll);
        String event = request.reconciliationMatched()
                ? "PAYROLL_RECONCILED" : "PAYROLL_RECONCILIATION_MISMATCH";
        auditService.log(event, "PAYROLL", id, payroll.getReconciliationNote());
        eventService.record(event, "PAYROLL", id, eventData(payroll));
        return get(id);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse cancel(Long id) {
        return cancel(id, null, "Legacy cancellation");
    }

    private PayrollResponse cancel(Long id, Integer expectedVersion, String reason) {
        Payroll payroll = find(id);
        assertVersion(payroll, expectedVersion);
        if (payroll.getStatus() == PayrollStatus.APPROVED
                || payroll.getStatus() == PayrollStatus.PAYMENT_PENDING
                || payroll.getStatus() == PayrollStatus.PAID) {
            throw new BusinessException("Approved or paid payroll cannot be cancelled", "USER_010");
        }
        if (payroll.getStatus() == PayrollStatus.CANCELLED) {
            return get(id);
        }
        if (reason == null || reason.isBlank() || reason.trim().length() < 5) {
            throw new BusinessException("Cancellation reason must contain at least 5 characters", "USER_008");
        }
        payroll.setStatus(PayrollStatus.CANCELLED);
        payroll.setCancelledBy(CurrentActor.accountId());
        payroll.setCancellationReason(reason.trim());
        payrollRepository.save(payroll);
        auditService.log("PAYROLL_CANCELLED", "PAYROLL", id, reason.trim());
        return get(id);
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollResponse applyAction(Long id, PayrollActionRequest request) {
        return switch (request.type()) {
            case APPROVE -> approve(id, request.expectedVersion(), normalizeReason(request.reason(), "Approved"));
            case SUBMIT_PAYMENT -> submitPayment(id, request.expectedVersion(), request.bankBatchReference(),
                    normalizeReason(request.reason(), "Payment batch submitted"));
            case RECONCILE -> reconcile(id, request);
            case CANCEL -> cancel(id, request.expectedVersion(), request.reason());
        };
    }

    @Transactional
    @CacheEvict(value = "userDashboard", allEntries = true)
    public PayrollGenerationResponse generateFromTimekeeping(PayrollGenerationRequest request) {
        LocalDate month = parseMonth(request.month());
        LocalDateTime from = month.atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atStartOfDay();
        List<Employee> employees = employeeRepository.findByStatusAndIsDeletedFalse(EmployeeStatus.ACTIVE);
        List<Long> ids = employees.stream().map(Employee::getAccountId).toList();
        Map<Long, List<WorkShift>> shifts = shiftRepository
                .findByEmployeeAccountIdInAndScheduledStartGreaterThanEqualAndScheduledStartLessThanAndStatusIn(
                        ids, from, to, EnumSet.of(ShiftStatus.SCHEDULED, ShiftStatus.COMPLETED))
                .stream().collect(Collectors.groupingBy(item -> item.getEmployee().getAccountId()));
        Map<Long, List<AttendanceRecord>> attendance = attendanceRepository
                .findByEmployeeAccountIdInAndShiftScheduledStartGreaterThanEqualAndShiftScheduledStartLessThan(
                        ids, from, to)
                .stream().collect(Collectors.groupingBy(item -> item.getEmployee().getAccountId()));
        Map<Long, List<LeaveRequest>> leaves = leaveRepository
                .findByEmployeeAccountIdInAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        ids, LeaveStatus.APPROVED, month.plusMonths(1).minusDays(1), month)
                .stream().collect(Collectors.groupingBy(item -> item.getEmployee().getAccountId()));

        int skippedExisting = 0;
        int skippedNoSchedule = 0;
        List<Long> generated = new ArrayList<>();
        for (Employee employee : employees) {
            if (payrollRepository.existsByEmployeeAccountIdAndSalaryMonth(employee.getAccountId(), month)) {
                skippedExisting++;
                continue;
            }
            List<WorkShift> employeeShifts = shifts.getOrDefault(employee.getAccountId(), List.of());
            if (employeeShifts.isEmpty()) {
                skippedNoSchedule++;
                continue;
            }
            Payroll payroll = payrollFromTimekeeping(employee, month, employeeShifts,
                    attendance.getOrDefault(employee.getAccountId(), List.of()),
                    leaves.getOrDefault(employee.getAccountId(), List.of()));
            payroll = payrollRepository.save(payroll);
            generated.add(payroll.getId());
            auditService.log("PAYROLL_GENERATED_FROM_TIMEKEEPING", "PAYROLL", payroll.getId(),
                    "sourceChecksum=" + payroll.getSourceChecksum());
            eventService.record("PAYROLL_GENERATED_FROM_TIMEKEEPING", "PAYROLL", payroll.getId(),
                    eventData(payroll));
        }
        return new PayrollGenerationResponse(month, generated.size(), skippedExisting, skippedNoSchedule, generated);
    }

    private Payroll payrollFromTimekeeping(Employee employee, LocalDate month, List<WorkShift> shifts,
                                             List<AttendanceRecord> attendance,
                                             List<LeaveRequest> leaves) {
        Map<Long, AttendanceRecord> byShift = attendance.stream()
                .collect(Collectors.toMap(item -> item.getShift().getId(), Function.identity(), (a, b) -> a));
        int scheduledMinutes = 0;
        int workedMinutes = 0;
        int overtimeMinutes = 0;
        int paidLeaveMinutes = 0;
        for (WorkShift shift : shifts) {
            int scheduled = Math.toIntExact(Duration.between(shift.getScheduledStart(), shift.getScheduledEnd()).toMinutes());
            scheduledMinutes += scheduled;
            AttendanceRecord record = byShift.get(shift.getId());
            if (record != null && record.getCheckOutAt() != null) {
                workedMinutes += Math.min(scheduled, record.getWorkedMinutes());
                overtimeMinutes += record.getOvertimeMinutes();
            } else if (isCoveredByPaidLeave(shift, leaves)) {
                paidLeaveMinutes += scheduled;
            }
        }
        int absentMinutes = Math.max(0, scheduledMinutes - workedMinutes - paidLeaveMinutes);
        BigDecimal base = employee.getBaseSalary();
        BigDecimal minuteRate = base.divide(BigDecimal.valueOf(26L * 8L * 60L), 8, RoundingMode.HALF_UP);
        BigDecimal overtime = minuteRate.multiply(BigDecimal.valueOf(overtimeMinutes))
                .multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deduction = minuteRate.multiply(BigDecimal.valueOf(absentMinutes))
                .setScale(2, RoundingMode.HALF_UP);

        Payroll payroll = new Payroll();
        payroll.setEmployee(employee);
        payroll.setSalaryMonth(month);
        payroll.setBasicSalary(base);
        payroll.setAllowance(overtime);
        payroll.setBonus(BigDecimal.ZERO);
        payroll.setDeduction(deduction);
        payroll.setTotalSalary(base.add(overtime).subtract(deduction).max(BigDecimal.ZERO));
        payroll.setStatus(PayrollStatus.PENDING_APPROVAL);
        payroll.setCreatedBy(currentActorOrNull());
        payroll.setSourceType(PayrollSourceType.TIMEKEEPING);
        payroll.setScheduledMinutes(scheduledMinutes);
        payroll.setWorkedMinutes(workedMinutes);
        payroll.setPaidLeaveMinutes(paidLeaveMinutes);
        payroll.setOvertimeMinutes(overtimeMinutes);
        payroll.setSourceChecksum(sourceChecksum(employee.getAccountId(), month, shifts, attendance, leaves));
        if (overtime.signum() > 0) {
            addDetail(payroll, PayrollDetailType.ALLOWANCE, "Tăng ca từ dữ liệu chấm công", overtime);
        }
        if (deduction.signum() > 0) {
            addDetail(payroll, PayrollDetailType.DEDUCTION, "Thiếu giờ không được nghỉ hưởng lương", deduction);
        }
        return payroll;
    }

    private boolean isCoveredByPaidLeave(WorkShift shift, List<LeaveRequest> leaves) {
        LocalDate day = shift.getScheduledStart().toLocalDate();
        return leaves.stream().anyMatch(item -> item.getLeaveType().isPaid()
                && !day.isBefore(item.getStartDate()) && !day.isAfter(item.getEndDate()));
    }

    private void addDetail(Payroll payroll, PayrollDetailType type, String description, BigDecimal amount) {
        PayrollDetail detail = new PayrollDetail();
        detail.setPayroll(payroll);
        detail.setType(type);
        detail.setDescription(description);
        detail.setAmount(amount);
        payroll.getDetails().add(detail);
    }

    private String sourceChecksum(Long employeeId, LocalDate month, List<WorkShift> shifts,
                                  List<AttendanceRecord> attendance, List<LeaveRequest> leaves) {
        try {
            List<String> parts = new ArrayList<>();
            parts.add("employee=" + employeeId);
            parts.add("month=" + month);
            shifts.stream().sorted(Comparator.comparing(WorkShift::getId))
                    .forEach(item -> parts.add("s=" + item.getId() + ":" + item.getVersion()));
            attendance.stream().sorted(Comparator.comparing(AttendanceRecord::getId))
                    .forEach(item -> parts.add("a=" + item.getId() + ":" + item.getVersion()));
            leaves.stream().sorted(Comparator.comparing(LeaveRequest::getId))
                    .forEach(item -> parts.add("l=" + item.getId() + ":" + item.getVersion()));
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("|", parts).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private Payroll find(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Payroll not found", "USER_006"));
    }

    private void assertVersion(Payroll payroll, Integer expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(payroll.getVersion())) {
            throw new BusinessException("Payroll was changed by another operator", "USER_VERSION_CONFLICT");
        }
    }

    private String normalizeReason(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason.trim();
    }

    private Long currentActorOrNull() {
        try {
            return CurrentActor.accountId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private LocalDate parseMonth(String value) {
        try {
            return YearMonth.parse(value).atDay(1);
        } catch (RuntimeException exception) {
            throw new BusinessException("Salary month must use YYYY-MM", "USER_008");
        }
    }

    private void applyAmounts(Payroll payroll, PayrollRequest request) {
        if (request.basicSalary() == null || request.basicSalary().signum() <= 0
                || request.allowance() == null || request.allowance().signum() < 0
                || request.bonus() == null || request.bonus().signum() < 0
                || request.deduction() == null || request.deduction().signum() < 0) {
            throw new BusinessException("Payroll amounts are invalid", "USER_008");
        }
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

    private Pageable sanitize(Pageable pageable) {
        return com.project.userservice.util.PageableUtils.sanitize(pageable,
                java.util.Set.of("id", "salaryMonth", "basicSalary", "totalSalary",
                        "status", "createdAt", "updatedAt"),
                "salaryMonth", org.springframework.data.domain.Sort.Direction.DESC);
    }
}
