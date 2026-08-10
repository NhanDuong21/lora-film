package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.AnalyticsDataQualityDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsDataQualityDailyRepository
        extends JpaRepository<AnalyticsDataQualityDaily, Long> {
    Optional<AnalyticsDataQualityDaily> findByStatDateAndSourceServiceAndEventType(
            LocalDate statDate, String sourceService, String eventType);
    List<AnalyticsDataQualityDaily> findAllByStatDateBetweenOrderByStatDateDesc(
            LocalDate startDate, LocalDate endDate);
    Optional<AnalyticsDataQualityDaily> findFirstByOrderByStatDateDescCalculatedAtDesc();
}
