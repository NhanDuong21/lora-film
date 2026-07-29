package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.AnalyticsJobRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalyticsJobRunRepository extends JpaRepository<AnalyticsJobRun, Long> {
    Optional<AnalyticsJobRun> findByRequestId(String requestId);
    List<AnalyticsJobRun> findTop20ByOrderByRequestedAtDesc();
}
