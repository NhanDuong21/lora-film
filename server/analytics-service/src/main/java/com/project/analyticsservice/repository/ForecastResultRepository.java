package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.ForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForecastResultRepository extends JpaRepository<ForecastResult, Long> {
    Optional<ForecastResult> findByEntityTypeAndEntityKeyAndForecastDateAndForecastType(
            String entityType, String entityKey, LocalDate forecastDate, String forecastType);
    List<ForecastResult> findAllByForecastDateBetweenOrderByForecastDateAsc(
            LocalDate startDate, LocalDate endDate);
}
