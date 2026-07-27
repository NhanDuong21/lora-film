package com.project.userservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotNull Long accountId,
        @NotNull Long departmentId,
        @NotNull Long positionId,
        @NotNull @PastOrPresent LocalDate hireDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal baseSalary
) {
}
