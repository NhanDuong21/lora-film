package com.project.userservice.dto.response;

import com.project.userservice.enumtype.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollResponse(
        Long id,
        Long employeeId,
        String employeeCode,
        String employeeName,
        LocalDate salaryMonth,
        BigDecimal basicSalary,
        BigDecimal allowance,
        BigDecimal bonus,
        BigDecimal deduction,
        BigDecimal totalSalary,
        PayrollStatus status,
        Long approvedBy,
        LocalDateTime approvedAt,
        LocalDateTime paidAt,
        List<PayrollDetailResponse> details
) {
}
