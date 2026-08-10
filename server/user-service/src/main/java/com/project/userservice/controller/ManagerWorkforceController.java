package com.project.userservice.controller;

import com.project.userservice.dto.request.LeaveActionRequest;
import com.project.userservice.dto.request.ShiftCancellationRequest;
import com.project.userservice.dto.request.WorkShiftRequest;
import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.AttendanceResponse;
import com.project.userservice.dto.response.EmployeeResponse;
import com.project.userservice.dto.response.LeaveResponse;
import com.project.userservice.dto.response.WorkShiftResponse;
import com.project.userservice.enumtype.LeaveStatus;
import com.project.userservice.service.ManagerWorkforceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/manager")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerWorkforceController {
    private final ManagerWorkforceService service;

    public ManagerWorkforceController(ManagerWorkforceService service) {
        this.service = service;
    }

    @GetMapping("/staff")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> staff(
            @RequestParam String cinemaPublicId) {
        return ResponseEntity.ok(ApiResponse.success("Danh sách nhân viên tại rạp",
                service.staff(cinemaPublicId)));
    }

    @GetMapping("/shifts")
    public ResponseEntity<ApiResponse<List<WorkShiftResponse>>> shifts(
            @RequestParam String cinemaPublicId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Danh sách ca làm tại rạp",
                service.shifts(cinemaPublicId, from, to)));
    }

    @PostMapping("/shifts")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> createShift(
            @RequestParam String cinemaPublicId,
            @Valid @RequestBody WorkShiftRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Đã xếp ca làm",
                service.createShift(cinemaPublicId, request)));
    }

    @PostMapping("/shifts/{shiftId}/cancel")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> cancelShift(
            @RequestParam String cinemaPublicId,
            @PathVariable Long shiftId,
            @Valid @RequestBody ShiftCancellationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã hủy ca làm",
                service.cancelShift(cinemaPublicId, shiftId, request)));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> attendance(
            @RequestParam String cinemaPublicId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Dữ liệu chấm công tại rạp",
                service.attendance(cinemaPublicId, from, to)));
    }

    @GetMapping("/leave-requests")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> leaves(
            @RequestParam String cinemaPublicId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("Đơn nghỉ của nhân viên tại rạp",
                service.leaves(cinemaPublicId, status, from, to)));
    }

    @PostMapping("/leave-requests/{leaveId}/actions")
    public ResponseEntity<ApiResponse<LeaveResponse>> leaveAction(
            @RequestParam String cinemaPublicId,
            @PathVariable Long leaveId,
            @Valid @RequestBody LeaveActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã xử lý đơn nghỉ",
                service.applyLeaveAction(cinemaPublicId, leaveId, request)));
    }
}
