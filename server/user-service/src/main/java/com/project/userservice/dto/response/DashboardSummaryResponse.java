package com.project.userservice.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalCustomers,
        long activeCustomers,
        long blockedCustomers,
        long totalEmployees,
        Map<String, Long> employeesByStatus,
        long pendingPayrolls,
        long approvedPayrolls,
        long paidPayrolls,
        BigDecimal totalPayrollCost
) {
}
