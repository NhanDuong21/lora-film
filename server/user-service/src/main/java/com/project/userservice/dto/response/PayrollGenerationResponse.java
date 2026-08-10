package com.project.userservice.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PayrollGenerationResponse(
        LocalDate salaryMonth,
        int generatedCount,
        int skippedExisting,
        int skippedNoSchedule,
        List<Long> payrollIds
) {
}
