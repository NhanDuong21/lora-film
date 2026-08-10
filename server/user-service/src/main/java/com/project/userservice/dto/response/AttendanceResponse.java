package com.project.userservice.dto.response;

import com.project.userservice.enumtype.AttendanceStatus;

import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long shiftId,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        AttendanceStatus status,
        Integer workedMinutes,
        Integer overtimeMinutes,
        String source,
        Long correctedBy,
        String correctionReason,
        Integer version
) {
}
