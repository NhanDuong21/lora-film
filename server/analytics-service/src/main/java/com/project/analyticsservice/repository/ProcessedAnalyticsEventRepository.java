package com.project.analyticsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.analyticsservice.entity.ProcessedAnalyticsEvent;

import java.util.Optional;

public interface ProcessedAnalyticsEventRepository extends JpaRepository<ProcessedAnalyticsEvent, Long> {
    boolean existsByEventId(String eventId);
    Optional<ProcessedAnalyticsEvent> findByEventId(String eventId);
    Optional<ProcessedAnalyticsEvent> findFirstByOrderByProcessedAtDesc();
}
