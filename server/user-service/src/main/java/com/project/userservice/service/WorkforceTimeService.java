package com.project.userservice.service;

import com.project.userservice.dto.request.*;
import com.project.userservice.dto.response.AttendanceResponse;
import com.project.userservice.dto.response.LeaveResponse;
import com.project.userservice.dto.response.WorkShiftResponse;
import com.project.userservice.entity.*;
import com.project.userservice.enumtype.*;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.*;
import com.project.userservice.security.CurrentActor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkforceTimeService {
    private static final int MAX_QUERY_DAYS = 62;
    private static final int LATE_GRACE_MINUTES = 5;
    private static final int MAX_ATTENDANCE_HOURS = 24;
    private static final int MAX_CHECK_OUT_DELAY_HOURS = 8;
    private static final int MAX_SHIFT_SEGMENTS = 8;
    private static final int MAX_DAILY_ASSIGNMENT_MINUTES = 16 * 60;

    private final WorkShiftRepository shiftRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;

    public WorkforceTimeService(WorkShiftRepository shiftRepository,
                                AttendanceRecordRepository attendanceRepository,
                                LeaveRequestRepository leaveRepository,
                                EmployeeRepository employeeRepository,
                                UserRepository userRepository,
                                UserAuditService auditService,
                                UserDomainEventService eventService) {
        this.shiftRepository = shiftRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public Page<WorkShiftResponse> shifts(Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        validateRange(from, to);
        Page<WorkShift> page = shiftRepository.search(employeeId, from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                sanitize(pageable, "scheduledStart"));
        Map<Long, User> users = users(page.getContent().stream()
                .map(item -> item.getEmployee().getAccountId()).toList());
        return page.map(item -> toShiftResponse(item, users.get(item.getEmployee().getAccountId())));
    }

    @Transactional(readOnly = true)
    public Page<WorkShiftResponse> myShifts(LocalDate from, LocalDate to, Pageable pageable) {
        return shifts(CurrentActor.accountId(), from, to, pageable);
    }

    @Transactional
    public WorkShiftResponse createShift(WorkShiftRequest request) {
        Employee employee = activeEmployeeForScheduling(request.employeeId());
        validateShiftTimes(request.scheduledStart(), request.scheduledEnd());
        if (!shiftRepository.findOverlaps(request.employeeId(), request.scheduledStart(), request.scheduledEnd()).isEmpty()) {
            throw new BusinessException("Employee already has an overlapping shift", "USER_SHIFT_OVERLAP");
        }
        WorkShift shift = new WorkShift();
        shift.setEmployee(employee);
        shift.setScheduledStart(request.scheduledStart());
        shift.setScheduledEnd(request.scheduledEnd());
        shift.setLocation(trim(request.location()));
        shift.setNote(trim(request.note()));
        shift.setCreatedBy(CurrentActor.accountId());
        shift = shiftRepository.save(shift);
        auditService.log("WORK_SHIFT_CREATED", "WORK_SHIFT", shift.getId(),
                "employeeId=" + employee.getAccountId());
        eventService.record("WORK_SHIFT_CREATED", "WORK_SHIFT", shift.getId(),
                Map.of("shiftId", shift.getId(), "employeeId", employee.getAccountId()));
        return toShiftResponse(shift, userRepository.findById(employee.getAccountId()).orElse(null));
    }

    @Transactional
    public List<WorkShiftResponse> createShiftBatch(WorkShiftBatchRequest request) {
        Employee employee = activeEmployeeForScheduling(request.employeeId());
        if (request.periods().size() > MAX_SHIFT_SEGMENTS) {
            throw new BusinessException("A shift assignment cannot contain more than 8 periods",
                    "USER_SHIFT_TOO_MANY_PERIODS");
        }

        List<WorkShiftPeriodRequest> periods = request.periods().stream()
                .sorted(Comparator.comparing(WorkShiftPeriodRequest::scheduledStart))
                .toList();
        long totalMinutes = 0;
        WorkShiftPeriodRequest previous = null;
        for (WorkShiftPeriodRequest period : periods) {
            validateShiftTimes(period.scheduledStart(), period.scheduledEnd());
            totalMinutes += Duration.between(period.scheduledStart(), period.scheduledEnd()).toMinutes();
            if (previous != null && period.scheduledStart().isBefore(previous.scheduledEnd())) {
                throw new BusinessException("Shift periods in the same assignment cannot overlap",
                        "USER_SHIFT_BATCH_OVERLAP");
            }
            if (!shiftRepository.findOverlaps(request.employeeId(), period.scheduledStart(),
                    period.scheduledEnd()).isEmpty()) {
                throw new BusinessException("Employee already has an overlapping shift",
                        "USER_SHIFT_OVERLAP");
            }
            previous = period;
        }
        if (totalMinutes > MAX_DAILY_ASSIGNMENT_MINUTES) {
            throw new BusinessException("Total assigned time cannot exceed 16 hours",
                    "USER_SHIFT_TOTAL_TOO_LONG");
        }

        User user = userRepository.findById(employee.getAccountId()).orElse(null);
        List<WorkShiftResponse> created = new java.util.ArrayList<>();
        for (WorkShiftPeriodRequest period : periods) {
            WorkShift shift = new WorkShift();
            shift.setEmployee(employee);
            shift.setScheduledStart(period.scheduledStart());
            shift.setScheduledEnd(period.scheduledEnd());
            shift.setLocation(request.location().trim());
            shift.setNote(trim(request.note()));
            shift.setCreatedBy(CurrentActor.accountId());
            shift = shiftRepository.save(shift);
            auditService.log("WORK_SHIFT_CREATED", "WORK_SHIFT", shift.getId(),
                    "employeeId=" + employee.getAccountId() + ", batchSize=" + periods.size());
            eventService.record("WORK_SHIFT_CREATED", "WORK_SHIFT", shift.getId(),
                    Map.of("shiftId", shift.getId(), "employeeId", employee.getAccountId()));
            created.add(toShiftResponse(shift, user));
        }
        return List.copyOf(created);
    }

    @Transactional
    public WorkShiftResponse cancelShift(Long id, ShiftCancellationRequest request) {
        WorkShift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Work shift not found", "USER_SHIFT_NOT_FOUND"));
        assertVersion(shift.getVersion(), request.expectedVersion(), "Work shift");
        if (shift.getStatus() == ShiftStatus.COMPLETED) {
            throw new BusinessException("Completed shift cannot be cancelled", "USER_SHIFT_INVALID_STATE");
        }
        if (attendanceRepository.findByShiftId(id).isPresent()) {
            throw new BusinessException("Shift with attendance cannot be cancelled", "USER_SHIFT_HAS_ATTENDANCE");
        }
        shift.setStatus(ShiftStatus.CANCELLED);
        shift.setCancelledBy(CurrentActor.accountId());
        shift.setCancelledAt(LocalDateTime.now());
        shift.setCancellationReason(request.reason().trim());
        shiftRepository.save(shift);
        auditService.log("WORK_SHIFT_CANCELLED", "WORK_SHIFT", id, request.reason().trim());
        return toShiftResponse(shift, userRepository.findById(shift.getEmployee().getAccountId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> attendance(Long employeeId, LocalDate from, LocalDate to, Pageable pageable) {
        validateRange(from, to);
        Page<AttendanceRecord> page = attendanceRepository.search(employeeId, from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(), sanitize(pageable, "createdAt"));
        Map<Long, User> users = users(page.getContent().stream()
                .map(item -> item.getEmployee().getAccountId()).toList());
        return page.map(item -> toAttendanceResponse(item, users.get(item.getEmployee().getAccountId())));
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> myAttendance(LocalDate from, LocalDate to, Pageable pageable) {
        return attendance(CurrentActor.accountId(), from, to, pageable);
    }

    @Transactional
    public AttendanceResponse checkIn(AttendanceActionRequest request) {
        WorkShift shift = ownedShift(request.shiftId());
        if (shift.getStatus() != ShiftStatus.SCHEDULED) {
            throw new BusinessException("Only scheduled shifts can be checked in", "USER_ATTENDANCE_INVALID_STATE");
        }
        if (attendanceRepository.findByShiftId(shift.getId()).isPresent()) {
            throw new BusinessException("Shift is already checked in", "USER_ATTENDANCE_DUPLICATE");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(shift.getScheduledStart().minusHours(2)) || now.isAfter(shift.getScheduledEnd())) {
            throw new BusinessException("Check-in is outside the allowed shift window", "USER_ATTENDANCE_WINDOW");
        }
        AttendanceRecord record = new AttendanceRecord();
        record.setShift(shift);
        record.setEmployee(shift.getEmployee());
        record.setCheckInAt(now);
        record.setStatus(now.isAfter(shift.getScheduledStart().plusMinutes(LATE_GRACE_MINUTES))
                ? AttendanceStatus.LATE : AttendanceStatus.ON_TIME);
        record = attendanceRepository.save(record);
        auditService.log("ATTENDANCE_CHECKED_IN", "ATTENDANCE", record.getId(), null);
        return toAttendanceResponse(record, userRepository.findById(shift.getEmployee().getAccountId()).orElse(null));
    }

    @Transactional
    public AttendanceResponse checkOut(AttendanceActionRequest request) {
        WorkShift shift = ownedShift(request.shiftId());
        AttendanceRecord record = attendanceRepository.findByShiftId(shift.getId())
                .orElseThrow(() -> new BusinessException("Shift has not been checked in", "USER_ATTENDANCE_NOT_FOUND"));
        if (record.getCheckOutAt() != null) {
            throw new BusinessException("Shift is already checked out", "USER_ATTENDANCE_INVALID_STATE");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(record.getCheckInAt())) {
            throw new BusinessException("Check-out must be after check-in", "USER_ATTENDANCE_INVALID_TIME");
        }
        if (now.isAfter(shift.getScheduledEnd().plusHours(MAX_CHECK_OUT_DELAY_HOURS))) {
            throw new BusinessException("Late check-out requires an audited attendance correction",
                    "USER_ATTENDANCE_WINDOW");
        }
        validateAttendanceDuration(record.getCheckInAt(), now);
        applyAttendanceTimes(record, record.getCheckInAt(), now, AttendanceStatus.COMPLETED);
        shift.setStatus(ShiftStatus.COMPLETED);
        shiftRepository.save(shift);
        attendanceRepository.save(record);
        auditService.log("ATTENDANCE_CHECKED_OUT", "ATTENDANCE", record.getId(), null);
        return toAttendanceResponse(record, userRepository.findById(shift.getEmployee().getAccountId()).orElse(null));
    }

    @Transactional
    public AttendanceResponse correctAttendance(Long shiftId, AttendanceCorrectionRequest request) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException("Work shift not found", "USER_SHIFT_NOT_FOUND"));
        if (shift.getStatus() == ShiftStatus.CANCELLED) {
            throw new BusinessException("Cancelled shift cannot receive attendance", "USER_SHIFT_INVALID_STATE");
        }
        if (!request.checkOutAt().isAfter(request.checkInAt())) {
            throw new BusinessException("Check-out must be after check-in", "USER_ATTENDANCE_INVALID_TIME");
        }
        validateAttendanceDuration(request.checkInAt(), request.checkOutAt());
        java.util.Optional<AttendanceRecord> existingRecord = attendanceRepository.findByShiftId(shiftId);
        if (existingRecord.isPresent() && request.expectedVersion() == null) {
            throw new BusinessException("Attendance version is required for an existing record",
                    "USER_VERSION_REQUIRED");
        }
        AttendanceRecord record = existingRecord.orElseGet(() -> {
            AttendanceRecord created = new AttendanceRecord();
            created.setShift(shift);
            created.setEmployee(shift.getEmployee());
            return created;
        });
        assertVersion(record.getVersion(), request.expectedVersion(), "Attendance");
        applyAttendanceTimes(record, request.checkInAt(), request.checkOutAt(), AttendanceStatus.CORRECTED);
        record.setSource("ADMIN_CORRECTION");
        record.setCorrectedBy(CurrentActor.accountId());
        record.setCorrectionReason(request.reason().trim());
        shift.setStatus(ShiftStatus.COMPLETED);
        shiftRepository.save(shift);
        record = attendanceRepository.save(record);
        auditService.log("ATTENDANCE_CORRECTED", "ATTENDANCE", record.getId(), request.reason().trim());
        return toAttendanceResponse(record, userRepository.findById(shift.getEmployee().getAccountId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse> leaves(Long employeeId, LeaveStatus status, LocalDate from, LocalDate to,
                                      Pageable pageable) {
        validateRange(from, to);
        Page<LeaveRequest> page = leaveRepository.search(employeeId, status, from, to,
                sanitize(pageable, "createdAt"));
        Map<Long, User> users = users(page.getContent().stream()
                .map(item -> item.getEmployee().getAccountId()).toList());
        return page.map(item -> toLeaveResponse(item, users.get(item.getEmployee().getAccountId())));
    }

    @Transactional(readOnly = true)
    public Page<LeaveResponse> myLeaves(LeaveStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        return leaves(CurrentActor.accountId(), status, from, to, pageable);
    }

    @Transactional
    public LeaveResponse createLeave(LeaveCreateRequest request) {
        Long accountId = CurrentActor.accountId();
        Employee employee = activeEmployee(accountId);
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException("Leave end date must not precede start date", "USER_LEAVE_INVALID_RANGE");
        }
        if (java.time.temporal.ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 62) {
            throw new BusinessException("A leave request cannot exceed 63 days", "USER_LEAVE_INVALID_RANGE");
        }
        if (!leaveRepository.findOverlaps(accountId,
                EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                request.startDate(), request.endDate()).isEmpty()) {
            throw new BusinessException("Leave request overlaps an existing request", "USER_LEAVE_OVERLAP");
        }
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(request.leaveType());
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setReason(request.reason().trim());
        leave = leaveRepository.save(leave);
        auditService.log("LEAVE_REQUESTED", "LEAVE_REQUEST", leave.getId(), request.reason().trim());
        return toLeaveResponse(leave, userRepository.findById(accountId).orElse(null));
    }

    @Transactional
    public LeaveResponse applyLeaveAction(Long id, LeaveActionRequest request) {
        LeaveRequest leave = leaveRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Leave request not found", "USER_LEAVE_NOT_FOUND"));
        assertVersion(leave.getVersion(), request.expectedVersion(), "Leave request");
        Long actor = CurrentActor.accountId();
        if (request.type() == LeaveActionType.CANCEL) {
            if (!leave.getEmployee().getAccountId().equals(actor)) {
                throw new BusinessException("Only the requester can cancel leave", "USER_LEAVE_FORBIDDEN");
            }
            if (leave.getStatus() != LeaveStatus.PENDING) {
                throw new BusinessException("Only pending leave can be cancelled", "USER_LEAVE_INVALID_STATE");
            }
            leave.setStatus(LeaveStatus.CANCELLED);
        } else {
            if (leave.getEmployee().getAccountId().equals(actor)) {
                throw new BusinessException("Requester cannot review their own leave", "USER_LEAVE_MAKER_CHECKER");
            }
            if (leave.getStatus() != LeaveStatus.PENDING) {
                throw new BusinessException("Only pending leave can be reviewed", "USER_LEAVE_INVALID_STATE");
            }
            if (request.type() == LeaveActionType.REJECT
                    && (request.note() == null || request.note().trim().length() < 5)) {
                throw new BusinessException("Rejection note must contain at least 5 characters", "USER_008");
            }
            leave.setStatus(request.type() == LeaveActionType.APPROVE
                    ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
            leave.setReviewedBy(actor);
            leave.setReviewedAt(LocalDateTime.now());
            leave.setReviewNote(trim(request.note()));
        }
        leaveRepository.save(leave);
        auditService.log("LEAVE_" + leave.getStatus().name(), "LEAVE_REQUEST", id, trim(request.note()));
        eventService.record("LEAVE_" + leave.getStatus().name(), "LEAVE_REQUEST", id,
                Map.of("leaveRequestId", id, "employeeId", leave.getEmployee().getAccountId()));
        return toLeaveResponse(leave,
                userRepository.findById(leave.getEmployee().getAccountId()).orElse(null));
    }

    private WorkShift ownedShift(Long shiftId) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException("Work shift not found", "USER_SHIFT_NOT_FOUND"));
        if (!shift.getEmployee().getAccountId().equals(CurrentActor.accountId())) {
            throw new BusinessException("Shift does not belong to current employee", "USER_SHIFT_FORBIDDEN");
        }
        return shift;
    }

    private Employee activeEmployee(Long accountId) {
        Employee employee = employeeRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Employee not found", "USER_003"));
        if (employee.isDeleted() || employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("Operation requires an active employee", "USER_010");
        }
        return employee;
    }

    private Employee activeEmployeeForScheduling(Long accountId) {
        if (accountId.equals(CurrentActor.accountId()) && CurrentActor.hasRole("ADMIN")) {
            throw new BusinessException("System administrator accounts cannot be assigned work shifts",
                    "USER_SHIFT_ADMIN_NOT_SCHEDULABLE");
        }
        Employee employee = employeeRepository.findByAccountIdForScheduling(accountId)
                .orElseThrow(() -> new BusinessException("Employee not found", "USER_003"));
        if (employee.isDeleted() || employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException("Operation requires an active employee", "USER_010");
        }
        return employee;
    }

    private void applyAttendanceTimes(AttendanceRecord record, LocalDateTime checkIn,
                                      LocalDateTime checkOut, AttendanceStatus status) {
        long worked = Duration.between(checkIn, checkOut).toMinutes();
        long scheduled = Duration.between(record.getShift().getScheduledStart(),
                record.getShift().getScheduledEnd()).toMinutes();
        record.setCheckInAt(checkIn);
        record.setCheckOutAt(checkOut);
        record.setWorkedMinutes(Math.toIntExact(Math.max(0, worked)));
        record.setOvertimeMinutes(Math.toIntExact(Math.max(0, worked - scheduled)));
        record.setStatus(status);
    }

    private void validateShiftTimes(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start) || Duration.between(start, end).toHours() > 16) {
            throw new BusinessException("Shift must be positive and no longer than 16 hours",
                    "USER_SHIFT_INVALID_TIME");
        }
    }

    private void validateAttendanceDuration(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (Duration.between(checkIn, checkOut).compareTo(Duration.ofHours(MAX_ATTENDANCE_HOURS)) > 0) {
            throw new BusinessException("Attendance duration cannot exceed 24 hours",
                    "USER_ATTENDANCE_INVALID_TIME");
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from) || from.plusDays(MAX_QUERY_DAYS).isBefore(to)) {
            throw new BusinessException("Date range must contain at most 63 days", "USER_008");
        }
    }

    private void assertVersion(Integer current, Integer expected, String resource) {
        if (expected != null && !expected.equals(current)) {
            throw new BusinessException(resource + " was changed by another operator", "USER_VERSION_CONFLICT");
        }
    }

    private Map<Long, User> users(java.util.List<Long> accountIds) {
        return userRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(User::getAccountId, Function.identity()));
    }

    WorkShiftResponse toShiftResponse(WorkShift shift, User user) {
        return new WorkShiftResponse(shift.getId(), shift.getEmployee().getAccountId(),
                shift.getEmployee().getEmployeeCode(), user == null ? null : user.getFullName(),
                shift.getScheduledStart(), shift.getScheduledEnd(), shift.getLocation(), shift.getNote(),
                shift.getStatus(), shift.getCreatedBy(), shift.getCancelledBy(), shift.getCancelledAt(),
                shift.getCancellationReason(), shift.getVersion());
    }

    AttendanceResponse toAttendanceResponse(AttendanceRecord item, User user) {
        return new AttendanceResponse(item.getId(), item.getShift().getId(), item.getEmployee().getAccountId(),
                item.getEmployee().getEmployeeCode(), user == null ? null : user.getFullName(),
                item.getShift().getScheduledStart(), item.getShift().getScheduledEnd(),
                item.getCheckInAt(), item.getCheckOutAt(), item.getStatus(), item.getWorkedMinutes(),
                item.getOvertimeMinutes(), item.getSource(), item.getCorrectedBy(), item.getCorrectionReason(),
                item.getVersion());
    }

    LeaveResponse toLeaveResponse(LeaveRequest item, User user) {
        return new LeaveResponse(item.getId(), item.getEmployee().getAccountId(),
                item.getEmployee().getEmployeeCode(), user == null ? null : user.getFullName(),
                item.getLeaveType(), item.getLeaveType().isPaid(), item.getStartDate(), item.getEndDate(),
                item.getReason(), item.getStatus(), item.getReviewedBy(), item.getReviewedAt(),
                item.getReviewNote(), item.getVersion(), item.getCreatedAt());
    }

    private Pageable sanitize(Pageable pageable, String fallbackSort) {
        return com.project.userservice.util.PageableUtils.sanitize(pageable,
                java.util.Set.of("id", "scheduledStart", "createdAt", "updatedAt"),
                fallbackSort, org.springframework.data.domain.Sort.Direction.DESC);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
