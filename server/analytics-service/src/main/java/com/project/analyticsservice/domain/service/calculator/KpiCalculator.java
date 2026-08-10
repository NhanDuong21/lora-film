package com.project.analyticsservice.domain.service.calculator;

import java.time.LocalDate;

public interface KpiCalculator {
    String stage();
    void calculate(LocalDate statDate);
}
