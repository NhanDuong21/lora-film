package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.FactBookingMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactBookingMetricRepository extends JpaRepository<FactBookingMetric, Long> {
    List<FactBookingMetric> findAllByBusinessDate(LocalDate businessDate);
    List<FactBookingMetric> findAllByBusinessDateBetween(LocalDate startDate, LocalDate endDate);
    List<FactBookingMetric> findAllByBusinessDateLessThanEqual(LocalDate endDate);
    Optional<FactBookingMetric> findFirstByBookingPublicIdOrderByOccurredAtDesc(String bookingPublicId);
}
