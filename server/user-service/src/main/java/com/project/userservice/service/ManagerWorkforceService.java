package com.project.userservice.service;

import com.project.userservice.dto.request.LeaveActionRequest;
import com.project.userservice.dto.request.ShiftCancellationRequest;
import com.project.userservice.dto.request.WorkShiftRequest;
import com.project.userservice.dto.response.AttendanceResponse;
import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.dto.response.LeaveResponse;
import com.project.userservice.dto.response.WorkShiftResponse;
import com.project.userservice.entity.Employee;
import com.project.userservice.entity.LeaveRequest;
import com.project.userservice.entity.User;
import com.project.userservice.entity.WorkShift;
import com.project.userservice.enumtype.LeaveStatus;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.exception.ForbiddenException;
import com.project.userservice.mapper.EmployeeMapper;
import com.project.userservice.repository.AttendanceRecordRepository;
import com.project.userservice.repository.EmployeeRepository;
import com.project.userservice.repository.LeaveRequestRepository;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.repository.WorkShiftRepository;
import com.project.userservice.security.ManagerCinemaScopeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagerWorkforceService {
    private static final int MAX_RANGE_DAYS = 63;

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final WorkShiftRepository shiftRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final LeaveRequestRepository leaveRepository;
    private final EmployeeMapper employeeMapper;
    private final WorkforceTimeService workforceTimeService;
    private final ManagerCinemaScopeService cinemaScope;

    public ManagerWorkforceService(EmployeeRepository employeeRepository,
                                   UserRepository userRepository,
                                   WorkShiftRepository shiftRepository,
                                   AttendanceRecordRepository attendanceRepository,
                                   LeaveRequestRepository leaveRepository,
                                   EmployeeMapper employeeMapper,
                                   WorkforceTimeService workforceTimeService,
                                   ManagerCinemaScopeService cinemaScope) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.shiftRepository = shiftRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.employeeMapper = employeeMapper;
        this.workforceTimeService = workforceTimeService;
        this.cinemaScope = cinemaScope;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> staff(String cinemaPublicId) {
        String cinema = requireCinema(cinemaPublicId);
        List<Employee> employees = employeeRepository
                .findByCinemaPublicIdAndIsDeletedFalseOrderByEmployeeCodeAsc(cinema);
        Map<Long, User> users = users(employees.stream().map(Employee::getAccountId).toList());
        return employees.stream()
                .map(employee -> employeeMapper.toResponse(employee, users.get(employee.getAccountId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkShiftResponse> shifts(String cinemaPublicId, LocalDate from, LocalDate to) {
        List<Long> ids = employeeIds(cinemaPublicId);
        if (ids.isEmpty()) return List.of();
        DateTimes range = range(from, to);
        List<WorkShift> shifts = shiftRepository.findForEmployees(ids, range.from(), range.to());
        Map<Long, User> users = users(ids);
        return shifts.stream().map(shift -> workforceTimeService.toShiftResponse(
                shift, users.get(shift.getEmployee().getAccountId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> attendance(String cinemaPublicId, LocalDate from, LocalDate to) {
        List<Long> ids = employeeIds(cinemaPublicId);
        if (ids.isEmpty()) return List.of();
        DateTimes range = range(from, to);
        Map<Long, User> users = users(ids);
        return attendanceRepository
                .findByEmployeeAccountIdInAndShiftScheduledStartGreaterThanEqualAndShiftScheduledStartLessThan(
                        ids, range.from(), range.to())
                .stream()
                .sorted(Comparator.comparing(item -> item.getShift().getScheduledStart()))
                .map(item -> workforceTimeService.toAttendanceResponse(
                        item, users.get(item.getEmployee().getAccountId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> leaves(String cinemaPublicId, LeaveStatus status,
                                      LocalDate from, LocalDate to) {
        List<Long> ids = employeeIds(cinemaPublicId);
        if (ids.isEmpty()) return List.of();
        validateRange(from, to);
        Map<Long, User> users = users(ids);
        return leaveRepository.findForEmployees(ids, status, from, to).stream()
                .map(item -> workforceTimeService.toLeaveResponse(
                        item, users.get(item.getEmployee().getAccountId())))
                .toList();
    }

    @Transactional
    public WorkShiftResponse createShift(String cinemaPublicId, WorkShiftRequest request) {
        requireEmployee(cinemaPublicId, request.employeeId());
        return workforceTimeService.createShift(request);
    }

    @Transactional
    public WorkShiftResponse cancelShift(String cinemaPublicId, Long shiftId,
                                         ShiftCancellationRequest request) {
        WorkShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy ca làm", "USER_SHIFT_NOT_FOUND"));
        requireEmployee(cinemaPublicId, shift.getEmployee().getAccountId());
        return workforceTimeService.cancelShift(shiftId, request);
    }

    @Transactional
    public LeaveResponse applyLeaveAction(String cinemaPublicId, Long leaveId,
                                          LeaveActionRequest request) {
        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy đơn nghỉ", "USER_LEAVE_NOT_FOUND"));
        requireEmployee(cinemaPublicId, leave.getEmployee().getAccountId());
        return workforceTimeService.applyLeaveAction(leaveId, request);
    }

    private List<Long> employeeIds(String cinemaPublicId) {
        String cinema = requireCinema(cinemaPublicId);
        return employeeRepository.findByCinemaPublicIdAndIsDeletedFalseOrderByEmployeeCodeAsc(cinema)
                .stream().map(Employee::getAccountId).toList();
    }

    private void requireEmployee(String cinemaPublicId, Long accountId) {
        String cinema = requireCinema(cinemaPublicId);
        Employee employee = employeeRepository.findById(accountId)
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException("Không tìm thấy nhân viên", "USER_003"));
        if (!cinema.equalsIgnoreCase(employee.getCinemaPublicId())) {
            throw new ForbiddenException("Nhân viên không thuộc rạp được phân công");
        }
    }

    private String requireCinema(String cinemaPublicId) {
        cinemaScope.requireAssigned(cinemaPublicId);
        return cinemaPublicId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private DateTimes range(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return new DateTimes(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)
                || from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new BusinessException("Khoảng ngày không được vượt quá 63 ngày", "USER_008");
        }
    }

    private Map<Long, User> users(List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getAccountId, Function.identity()));
    }

    private record DateTimes(LocalDateTime from, LocalDateTime to) {}
}
