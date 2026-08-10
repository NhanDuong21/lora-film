package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.ForecastModelMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForecastModelMetricRepository extends JpaRepository<ForecastModelMetric, Long> {
    Optional<ForecastModelMetric>
            findByEntityTypeAndEntityKeyAndForecastTypeAndModelVersionAndEvaluationDate(
                    String entityType, String entityKey, String forecastType,
                    String modelVersion, LocalDate evaluationDate);
    List<ForecastModelMetric> findAllByEvaluationDateBetweenOrderByEvaluationDateDesc(
            LocalDate startDate, LocalDate endDate);
}
