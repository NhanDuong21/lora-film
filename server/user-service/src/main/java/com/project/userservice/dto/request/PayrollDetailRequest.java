package com.project.userservice.dto.request;

import com.project.userservice.enumtype.PayrollDetailType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PayrollDetailRequest(
        @NotNull PayrollDetailType type,
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin("0.00") BigDecimal amount
) {
}
