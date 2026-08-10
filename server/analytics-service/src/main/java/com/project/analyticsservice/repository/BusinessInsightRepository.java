package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.BusinessInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BusinessInsightRepository extends JpaRepository<BusinessInsight, Long> {
    Optional<BusinessInsight> findByFingerprint(String fingerprint);
    List<BusinessInsight> findAllByResolvedFalseAndStatDateBetweenOrderByCreatedAtDesc(
            LocalDate startDate, LocalDate endDate);
    List<BusinessInsight> findAllByStatDate(LocalDate statDate);
    List<BusinessInsight> findAllByResolvedFalse();
}
