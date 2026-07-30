package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.CinemaPerformanceDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CinemaPerformanceDailyRepository extends JpaRepository<CinemaPerformanceDaily, Long> {
    Optional<CinemaPerformanceDaily> findByCinemaKeyAndStatDate(String cinemaKey, LocalDate statDate);
    List<CinemaPerformanceDaily> findAllByStatDateBetween(LocalDate startDate, LocalDate endDate);
    List<CinemaPerformanceDaily> findAllByCinemaKeyAndStatDateBetweenOrderByStatDateAsc(
            String cinemaKey, LocalDate startDate, LocalDate endDate);
    Optional<CinemaPerformanceDaily> findFirstByCinemaKeyOrderByStatDateDesc(String cinemaKey);
}
