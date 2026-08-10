package com.project.userservice.dto.response;

import com.project.userservice.enumtype.LeaveStatus;
import com.project.userservice.enumtype.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LeaveType leaveType,
        boolean paid,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        LeaveStatus status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote,
        Integer version,
        LocalDateTime createdAt
) {
}
