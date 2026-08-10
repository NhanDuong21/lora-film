package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.AnalyticsHealthScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsHealthScoreRepository extends JpaRepository<AnalyticsHealthScore, Long> {
    Optional<AnalyticsHealthScore> findByEntityTypeAndEntityKeyAndStatDate(
            String entityType, String entityKey, LocalDate statDate);
    List<AnalyticsHealthScore> findAllByStatDateBetweenOrderByStatDateDesc(
            LocalDate startDate, LocalDate endDate);
}
