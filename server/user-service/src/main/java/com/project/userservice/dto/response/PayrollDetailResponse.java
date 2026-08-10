package com.project.userservice.dto.response;

import com.project.userservice.enumtype.PayrollDetailType;

import java.math.BigDecimal;

public record PayrollDetailResponse(
        Long id,
        PayrollDetailType type,
        String description,
        BigDecimal amount
) {
}
