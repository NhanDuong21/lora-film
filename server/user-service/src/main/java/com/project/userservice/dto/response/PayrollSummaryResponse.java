package com.project.userservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayrollSummaryResponse(
        LocalDate salaryMonth,
        long totalRecords,
        BigDecimal totalNetAmount,
        long pendingApproval,
        long approved,
        long paymentPending,
        long paid,
        long cancelled
) {
}
