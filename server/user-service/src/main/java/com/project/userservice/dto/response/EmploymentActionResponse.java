package com.project.userservice.dto.response;

import com.project.userservice.enumtype.EmploymentActionType;
import com.project.userservice.enumtype.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmploymentActionResponse(
        Long id,
        Long employeeAccountId,
        EmploymentActionType type,
        LocalDate effectiveDate,
        String reason,
        EmployeeStatus previousStatus,
        EmployeeStatus newStatus,
        Long previousDepartmentId,
        Long newDepartmentId,
        Long previousPositionId,
        Long newPositionId,
        BigDecimal previousBaseSalary,
        BigDecimal newBaseSalary,
        Long performedBy,
        LocalDateTime createdAt
) {
}
