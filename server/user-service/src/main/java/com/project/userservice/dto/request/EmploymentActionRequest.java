package com.project.userservice.dto.request;

import com.project.userservice.enumtype.EmploymentActionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmploymentActionRequest(
        @NotNull EmploymentActionType type,
        Long departmentId,
        Long positionId,
        @DecimalMin("0.01") BigDecimal baseSalary,
        @NotNull @PastOrPresent LocalDate effectiveDate,
        @NotBlank @Size(min = 5, max = 500) String reason,
        Integer expectedVersion
) {
}
