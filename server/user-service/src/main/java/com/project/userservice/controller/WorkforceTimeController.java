package com.project.userservice.controller;

import com.project.userservice.dto.request.*;
import com.project.userservice.dto.response.*;
import com.project.userservice.enumtype.LeaveStatus;
import com.project.userservice.service.WorkforceTimeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/workforce")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Workforce time and attendance")
public class WorkforceTimeController {
    private final WorkforceTimeService service;

    public WorkforceTimeController(WorkforceTimeService service) {
        this.service = service;
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<Page<WorkShiftResponse>>> shifts(
            @RequestParam(required = false) Long employeeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Work shifts retrieved",
                service.shifts(employeeId, from, to, pageable)));
    }

    @GetMapping("/shifts/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE','STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<WorkShiftResponse>>> myShifts(
            @RequestParam LocalDate from, @RequestParam LocalDate to, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("My work shifts retrieved",
                service.myShifts(from, to, pageable)));
    }

    @PostMapping("/shifts")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> createShift(
            @Valid @RequestBody WorkShiftRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Work shift created",
                service.createShift(request)));
    }

    @PostMapping("/shifts/batch")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<List<WorkShiftResponse>>> createShiftBatch(
            @Valid @RequestBody WorkShiftBatchRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Work shift periods created",
                service.createShiftBatch(request)));
    }

    @PostMapping("/shifts/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> cancelShift(
            @PathVariable Long id, @Valid @RequestBody ShiftCancellationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Work shift cancelled",
                service.cancelShift(id, request)));
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> attendance(
            @RequestParam(required = false) Long employeeId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Attendance retrieved",
                service.attendance(employeeId, from, to, pageable)));
    }

    @GetMapping("/attendance/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE','STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> myAttendance(
            @RequestParam LocalDate from, @RequestParam LocalDate to, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("My attendance retrieved",
                service.myAttendance(from, to, pageable)));
    }

    @PostMapping("/attendance/check-in")
    @PreAuthorize("hasAnyRole('EMPLOYEE','STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody AttendanceActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Checked in", service.checkIn(request)));
    }

    @PostMapping("/attendance/check-out")
    @PreAuthorize("hasAnyRole('EMPLOYEE','STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @Valid @RequestBody AttendanceActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Checked out", service.checkOut(request)));
    }

    @PostMapping("/attendance/{shiftId}/correction")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> correct(
            @PathVariable Long shiftId, @Valid @RequestBody AttendanceCorrectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Attendance corrected",
                service.correctAttendance(shiftId, request)));
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<ApiResponse<Page<LeaveResponse>>> leaves(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Leave requests retrieved",
                service.leaves(employeeId, status, from, to, pageable)));
    }

    @GetMapping("/leave-requests/me")
    @PreAuthorize("hasAnyRole('EMPLOYEE','STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Page<LeaveResponse>>> myLeaves(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("My leave requests retrieved",
                service.myLeaves(status, from, to, pageable)));
    }

    @PostMapping("/leave-requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE','STAFF','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<LeaveResponse>> createLeave(
            @Valid @RequestBody LeaveCreateRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Leave requested",
                service.createLeave(request)));
    }

    @PostMapping("/leave-requests/{id}/actions")
    @PreAuthorize("#request.type != null and ((#request.type.name() == 'CANCEL' and "
            + "hasAnyRole('ADMIN','MANAGER','EMPLOYEE','STAFF')) or "
            + "(#request.type.name() != 'CANCEL' and "
            + "(hasAnyRole('ADMIN','MANAGER') or hasAuthority('EMPLOYEE_UPDATE'))))")
    public ResponseEntity<ApiResponse<LeaveResponse>> leaveAction(
            @PathVariable Long id, @Valid @RequestBody LeaveActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Leave action recorded",
                service.applyLeaveAction(id, request)));
    }
}
