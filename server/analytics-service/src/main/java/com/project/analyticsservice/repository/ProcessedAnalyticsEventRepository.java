package com.project.analyticsservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.analyticsservice.entity.ProcessedAnalyticsEvent;

@Repository
public interface ProcessedAnalyticsEventRepository extends JpaRepository<ProcessedAnalyticsEvent, Long> {

    boolean existsByEventId(String eventId);

    Optional<ProcessedAnalyticsEvent> findByEventId(String eventId);

    List<ProcessedAnalyticsEvent> findAllByEventType(String eventType);
}
