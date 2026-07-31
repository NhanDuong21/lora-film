package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.AnomalyDetection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnomalyDetectionRepository extends JpaRepository<AnomalyDetection, Long> {
    Optional<AnomalyDetection> findByFingerprint(String fingerprint);
    List<AnomalyDetection> findAllByStatDate(LocalDate statDate);
    List<AnomalyDetection> findAllByStatusAndStatDateBetweenOrderByDetectedAtDesc(
            String status, LocalDate startDate, LocalDate endDate);
}
