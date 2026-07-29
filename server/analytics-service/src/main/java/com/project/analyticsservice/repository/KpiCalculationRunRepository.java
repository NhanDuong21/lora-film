package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.KpiCalculationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDate;

public interface KpiCalculationRunRepository extends JpaRepository<KpiCalculationRun, Long> {
    Optional<KpiCalculationRun> findFirstByOrderByStartedAtDesc();
    Optional<KpiCalculationRun> findFirstByStatDateAndStatusOrderByStartedAtDesc(
            LocalDate statDate, String status);
}
