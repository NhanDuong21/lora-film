package com.project.userservice.dto.response;

import com.project.userservice.enumtype.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
        Long accountId,
        String employeeCode,
        String fullName,
        String email,
        String phoneNumber,
        Long departmentId,
        String departmentCode,
        String departmentName,
        Long positionId,
        String positionCode,
        String positionName,
        LocalDate hireDate,
        BigDecimal baseSalary,
        String cinemaPublicId,
        EmployeeStatus status,
        Integer version
) {
}
