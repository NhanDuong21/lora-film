package com.project.userservice.service;

import com.project.userservice.dto.request.PayrollRequest;
import com.project.userservice.dto.request.PayrollActionRequest;
import com.project.userservice.dto.request.PayrollGenerationRequest;
import com.project.userservice.dto.request.WorkShiftRequest;
import com.project.userservice.dto.request.AttendanceCorrectionRequest;
import com.project.userservice.dto.response.PayrollResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.EmployeeStatus;
import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.enumtype.PayrollActionType;
import com.project.userservice.enumtype.PayrollSourceType;
import com.project.userservice.enumtype.ReconciliationStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.PositionRepository;
import com.project.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PayrollServiceIntegrationTest {
    @Autowired PayrollService payrollService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PositionRepository positionRepository;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired WorkforceTimeService workforceTimeService;

    @BeforeEach
    void seedEmployee() {
        User user = new User();
        user.setAccountId(9001L);
        user.setFullName("Payroll Test User");
        user.setPhoneNumber("0900009001");
        userRepository.save(user);

        Department department = new Department();
        department.setCode("TEST_FIN");
        department.setName("Test Finance");
        department = departmentRepository.save(department);

        Position position = new Position();
        position.setCode("TEST_ACCOUNTANT");
        position.setTitle("Test Accountant");
        position.setDepartment(department);
        position = positionRepository.save(position);

        Employee employee = new Employee();
        employee.setAccountId(user.getAccountId());
        employee.setEmployeeCode("EMP00009001");
        employee.setDepartment(department);
        employee.setPosition(position);
        employee.setBaseSalary(new BigDecimal("10000000"));
        employee.setHireDate(LocalDate.of(2025, 1, 1));
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(employee);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculatesSalaryAndEnforcesUniqueEmployeeMonth() {
        PayrollResponse created = payrollService.create(request("2026-07"));

        assertThat(created.totalSalary()).isEqualByComparingTo("11250000");
        assertThat(created.status()).isEqualTo(PayrollStatus.PENDING_APPROVAL);
        assertThatThrownBy(() -> payrollService.create(request("2026-07")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void payrollListSupportsSystemCreatedRecordsWithoutActor() {
        payrollService.create(request("2026-08"));

        var page = payrollService.search(9001L, null, "2026-08", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().createdBy()).isNull();
        assertThat(page.getContent().getFirst().createdByName()).isNull();
    }

    @Test
    void approvedPayrollCannotBeEdited() {
        PayrollResponse created = payrollService.create(request("2026-06"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));

        PayrollResponse approved = payrollService.approve(created.id());

        assertThat(approved.status()).isEqualTo(PayrollStatus.APPROVED);
        assertThatThrownBy(() -> payrollService.update(created.id(), request("2026-06")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only draft or pending");
    }

    @Test
    void cancelledPayrollIsImmutable() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(77L, null, List.of()));
        PayrollResponse created = payrollService.create(request("2026-03"));
        PayrollResponse cancelled = payrollService.cancel(created.id());

        assertThat(cancelled.status()).isEqualTo(PayrollStatus.CANCELLED);
        assertThatThrownBy(() -> payrollService.update(created.id(), request("2026-03")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only draft or pending");
    }

    @Test
    void payrollCreatorCannotApproveOwnRecord() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(77L, null, List.of()));
        PayrollResponse created = payrollService.create(request("2026-05"));

        assertThat(created.createdBy()).isEqualTo(77L);
        assertThatThrownBy(() -> payrollService.applyAction(created.id(),
                new PayrollActionRequest(PayrollActionType.APPROVE,
                        "Approve monthly payroll", null, null, null, null, created.version())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("creator cannot approve");
    }

    @Test
    void employeeCannotApproveTheirOwnPayroll() {
        PayrollResponse created = payrollService.create(request("2026-10"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(9001L, null, List.of()));

        assertThatThrownBy(() -> payrollService.applyAction(created.id(),
                new PayrollActionRequest(PayrollActionType.APPROVE,
                        "Reviewed by payroll beneficiary", null, null, null, null,
                        created.version())))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo("USER_PAYROLL_SELF_APPROVAL");
    }

    @Test
    void payrollDetailReturnsOperationalActorNames() {
        saveActor(77L, "Nguyễn Minh Anh");
        saveActor(88L, "Lê Thu Hà");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(77L, null, List.of()));
        PayrollResponse created = payrollService.create(request("2026-01"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(88L, null, List.of()));
        PayrollResponse approved = payrollService.applyAction(created.id(),
                new PayrollActionRequest(PayrollActionType.APPROVE, "Independent approval",
                        null, null, null, null, created.version()));

        assertThat(approved.createdByName()).isEqualTo("Nguyễn Minh Anh");
        assertThat(approved.approvedByName()).isEqualTo("Lê Thu Hà");
    }

    @Test
    void paymentIsPaidOnlyAfterBankAndAccountingReconciliation() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(77L, null, List.of()));
        PayrollResponse created = payrollService.create(request("2026-04"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(88L, null, List.of()));
        PayrollResponse approved = payrollService.applyAction(created.id(),
                new PayrollActionRequest(PayrollActionType.APPROVE, "Independent approval",
                        null, null, null, null, created.version()));
        PayrollResponse submitted = payrollService.applyAction(created.id(),
                new PayrollActionRequest(PayrollActionType.SUBMIT_PAYMENT, "Submitted to bank",
                        null, "BANK-BATCH-TEST-001", null, null, approved.version()));

        assertThat(submitted.status()).isEqualTo(PayrollStatus.PAYMENT_PENDING);
        assertThat(submitted.reconciliationStatus()).isEqualTo(ReconciliationStatus.PENDING);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(99L, null, List.of()));
        PayrollResponse paid = payrollService.applyAction(created.id(),
                new PayrollActionRequest(PayrollActionType.RECONCILE, "Bank and ledger matched",
                        "BANK-TXN-TEST-001", null, "GL-TEST-001", true, submitted.version()));

        assertThat(paid.status()).isEqualTo(PayrollStatus.PAID);
        assertThat(paid.reconciliationStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(paid.paidBy()).isEqualTo(99L);
    }

    @Test
    void generatesPayrollFromImmutableTimekeepingSnapshot() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(88L, null, List.of()));
        LocalDateTime start = LocalDateTime.of(2026, 2, 10, 8, 0);
        var shift = workforceTimeService.createShift(
                new WorkShiftRequest(9001L, start, start.plusHours(8), "Cinema Test", null));
        workforceTimeService.correctAttendance(shift.id(),
                new AttendanceCorrectionRequest(start, start.plusHours(9),
                        "Verified timekeeping source", null));

        var outcome = payrollService.generateFromTimekeeping(new PayrollGenerationRequest("2026-02"));
        PayrollResponse payroll = payrollService.get(outcome.payrollIds().getFirst());

        assertThat(outcome.generatedCount()).isEqualTo(1);
        assertThat(payroll.sourceType()).isEqualTo(PayrollSourceType.TIMEKEEPING);
        assertThat(payroll.scheduledMinutes()).isEqualTo(480);
        assertThat(payroll.workedMinutes()).isEqualTo(480);
        assertThat(payroll.overtimeMinutes()).isEqualTo(60);
        assertThat(payroll.sourceChecksum()).hasSize(64);
    }

    @Test
    void zeroAttendancePayrollRequiresExplicitReviewReason() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(77L, null, List.of()));
        LocalDateTime start = LocalDateTime.of(2026, 9, 10, 8, 0);
        workforceTimeService.createShift(
                new WorkShiftRequest(9001L, start, start.plusHours(8), "Cinema Test", null));
        var outcome = payrollService.generateFromTimekeeping(new PayrollGenerationRequest("2026-09"));
        PayrollResponse payroll = payrollService.get(outcome.payrollIds().getFirst());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(88L, null, List.of()));
        assertThatThrownBy(() -> payrollService.applyAction(payroll.id(),
                new PayrollActionRequest(PayrollActionType.APPROVE, "Too short",
                        null, null, null, null, payroll.version())))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo("USER_PAYROLL_ATTENDANCE_REVIEW_REQUIRED");

        PayrollResponse approved = payrollService.applyAction(payroll.id(),
                new PayrollActionRequest(PayrollActionType.APPROVE,
                        "Verified missing attendance evidence",
                        null, null, null, null, payroll.version()));
        assertThat(approved.status()).isEqualTo(PayrollStatus.APPROVED);
    }

    private PayrollRequest request(String month) {
        return new PayrollRequest(9001L, month, new BigDecimal("10000000"),
                new BigDecimal("500000"), new BigDecimal("1000000"),
                new BigDecimal("250000"), List.of());
    }

    private void saveActor(Long accountId, String fullName) {
        User actor = new User();
        actor.setAccountId(accountId);
        actor.setFullName(fullName);
        userRepository.save(actor);
    }
}
