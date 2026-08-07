package com.project.userservice.dto.response;

import com.project.userservice.enumtype.PayrollStatus;
import com.project.userservice.enumtype.PayrollSourceType;
import com.project.userservice.enumtype.ReconciliationStatus;

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
        Long createdBy,
        Long approvedBy,
        LocalDateTime approvedAt,
        Long paidBy,
        LocalDateTime paidAt,
        String paymentReference,
        String bankBatchReference,
        String accountingReference,
        ReconciliationStatus reconciliationStatus,
        Long reconciledBy,
        LocalDateTime reconciledAt,
        String reconciliationNote,
        PayrollSourceType sourceType,
        String sourceChecksum,
        Integer scheduledMinutes,
        Integer workedMinutes,
        Integer paidLeaveMinutes,
        Integer overtimeMinutes,
        Long cancelledBy,
        String cancellationReason,
        Integer version,
        List<PayrollDetailResponse> details
) {
}
