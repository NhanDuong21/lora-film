package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.FactBookingCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FactBookingCancellationRepository extends JpaRepository<FactBookingCancellation, Long> {
    List<FactBookingCancellation> findAllByBusinessDate(LocalDate businessDate);
    List<FactBookingCancellation> findAllByBusinessDateBetween(LocalDate startDate, LocalDate endDate);
}
