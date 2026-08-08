package com.project.userservice.service;

import com.project.userservice.dto.request.*;
import com.project.userservice.dto.response.WorkShiftResponse;
import com.project.userservice.entity.Department;
import com.project.userservice.entity.Position;
import com.project.userservice.entity.User;
import com.project.userservice.enumtype.*;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.DepartmentRepository;
import com.project.userservice.repository.PositionRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.WorkShiftRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class WorkforceTimeServiceIntegrationTest {
    @Autowired WorkforceTimeService workforceTimeService;
    @Autowired EmployeeService employeeService;
    @Autowired UserRepository userRepository;
    @Autowired DepartmentRepository departmentRepository;
    @Autowired PositionRepository positionRepository;
    @Autowired WorkShiftRepository workShiftRepository;

    private static final Long EMPLOYEE_ID = 9201L;

    @BeforeEach
    void seedEmployee() {
        User user = new User();
        user.setAccountId(EMPLOYEE_ID);
        user.setFullName("Timekeeping Test User");
        user.setEmail("timekeeping@example.com");
        user.setPhoneNumber("0909123456");
        user.setAccountType(AccountType.WORKFORCE);
        userRepository.save(user);

        Department department = new Department();
        department.setCode("TEST_TIME_OPS");
        department.setName("Test Time Operations");
        department = departmentRepository.save(department);

        Position position = new Position();
        position.setCode("TEST_TIME_STAFF");
        position.setTitle("Test Time Staff");
        position.setDepartment(department);
        position = positionRepository.save(position);

        employeeService.create(new EmployeeRequest(EMPLOYEE_ID, department.getId(), position.getId(),
                LocalDate.of(2026, 1, 1), new BigDecimal("12000000")));
        authenticate(8001L);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksOverlappingShiftAndPersistsAuditedAttendanceCorrection() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 10, 8, 0);
        WorkShiftResponse shift = workforceTimeService.createShift(
                new WorkShiftRequest(EMPLOYEE_ID, start, start.plusHours(8), "Cinema A", null));

        assertThatThrownBy(() -> workforceTimeService.createShift(
                new WorkShiftRequest(EMPLOYEE_ID, start.plusHours(1), start.plusHours(9), "Cinema A", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("overlapping");

        var corrected = workforceTimeService.correctAttendance(shift.id(),
                new AttendanceCorrectionRequest(start, start.plusHours(9),
                        "Supervisor verified camera log", null));

        assertThat(corrected.status()).isEqualTo(AttendanceStatus.CORRECTED);
        assertThat(corrected.workedMinutes()).isEqualTo(540);
        assertThat(corrected.overtimeMinutes()).isEqualTo(60);
        assertThat(corrected.correctedBy()).isEqualTo(8001L);

        assertThatThrownBy(() -> workforceTimeService.correctAttendance(shift.id(),
                new AttendanceCorrectionRequest(start, start.plusHours(8),
                        "Stale correction without version", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("version is required");
    }

    @Test
    void createsSplitShiftAtomicallyAsSeparateAttendancePeriods() {
        LocalDateTime morningStart = LocalDateTime.of(2026, 8, 11, 8, 0);
        LocalDateTime afternoonStart = LocalDateTime.of(2026, 8, 11, 14, 0);

        List<WorkShiftResponse> created = workforceTimeService.createShiftBatch(
                new WorkShiftBatchRequest(EMPLOYEE_ID, List.of(
                        new WorkShiftPeriodRequest(morningStart, morningStart.plusHours(4)),
                        new WorkShiftPeriodRequest(afternoonStart, afternoonStart.plusHours(4))
                ), "LoraFilm Quận 1", "Ca gãy theo lịch vận hành"));

        assertThat(created).hasSize(2);
        assertThat(created).extracting(WorkShiftResponse::location)
                .containsOnly("LoraFilm Quận 1");
        assertThat(created).extracting(WorkShiftResponse::scheduledStart)
                .containsExactly(morningStart, afternoonStart);
    }

    @Test
    void rejectsWholeSplitShiftWhenItsPeriodsOverlap() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 13, 8, 0);
        long countBefore = workShiftRepository.count();

        assertThatThrownBy(() -> workforceTimeService.createShiftBatch(
                new WorkShiftBatchRequest(EMPLOYEE_ID, List.of(
                        new WorkShiftPeriodRequest(start, start.plusHours(5)),
                        new WorkShiftPeriodRequest(start.plusHours(4), start.plusHours(8))
                ), "LoraFilm Quận 1", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot overlap");

        assertThat(workShiftRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void rejectsSplitShiftLongerThanDailyLimit() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 14, 0, 0);

        assertThatThrownBy(() -> workforceTimeService.createShiftBatch(
                new WorkShiftBatchRequest(EMPLOYEE_ID, List.of(
                        new WorkShiftPeriodRequest(start, start.plusHours(8)),
                        new WorkShiftPeriodRequest(start.plusHours(9), start.plusHours(18))
                ), "LoraFilm Quận 1", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot exceed 16 hours");
    }

    @Test
    void leaveRequiresDifferentReviewer() {
        authenticate(EMPLOYEE_ID);
        var leave = workforceTimeService.createLeave(new LeaveCreateRequest(LeaveType.ANNUAL,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 21), "Annual family leave"));

        assertThatThrownBy(() -> workforceTimeService.applyLeaveAction(leave.id(),
                new LeaveActionRequest(LeaveActionType.APPROVE, "Self approval", leave.version())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot review");

        authenticate(8002L);
        var approved = workforceTimeService.applyLeaveAction(leave.id(),
                new LeaveActionRequest(LeaveActionType.APPROVE, "Coverage confirmed", leave.version()));
        assertThat(approved.status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(approved.reviewedBy()).isEqualTo(8002L);
    }

    @Test
    void rejectsUnboundedAttendanceCorrection() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 12, 8, 0);
        WorkShiftResponse shift = workforceTimeService.createShift(
                new WorkShiftRequest(EMPLOYEE_ID, start, start.plusHours(8), "Cinema A", null));

        assertThatThrownBy(() -> workforceTimeService.correctAttendance(shift.id(),
                new AttendanceCorrectionRequest(start, start.plusHours(25),
                        "Invalid overnight attendance", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot exceed 24 hours");
    }

    private void authenticate(Long accountId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(accountId, null, List.of()));
    }
}
