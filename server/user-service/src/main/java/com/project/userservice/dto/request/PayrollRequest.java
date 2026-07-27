package com.project.userservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record PayrollRequest(
        @NotNull Long employeeId,
        @NotBlank @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$") String salaryMonth,
        @NotNull @DecimalMin("0.01") BigDecimal basicSalary,
        @NotNull @DecimalMin("0.00") BigDecimal allowance,
        @NotNull @DecimalMin("0.00") BigDecimal bonus,
        @NotNull @DecimalMin("0.00") BigDecimal deduction,
        List<@Valid PayrollDetailRequest> details
) {
}
