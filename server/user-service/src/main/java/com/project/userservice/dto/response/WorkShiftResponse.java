package com.project.userservice.dto.response;

import com.project.userservice.enumtype.ShiftStatus;

import java.time.LocalDateTime;

public record WorkShiftResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        String location,
        String note,
        ShiftStatus status,
        Long createdBy,
        Long cancelledBy,
        LocalDateTime cancelledAt,
        String cancellationReason,
        Integer version
) {
}
