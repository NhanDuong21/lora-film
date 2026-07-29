package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.MoviePerformanceDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MoviePerformanceDailyRepository extends JpaRepository<MoviePerformanceDaily, Long> {
    Optional<MoviePerformanceDaily> findByMovieKeyAndStatDate(String movieKey, LocalDate statDate);
    List<MoviePerformanceDaily> findAllByStatDateBetween(LocalDate startDate, LocalDate endDate);
    List<MoviePerformanceDaily> findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(
            Long movieId, LocalDate startDate, LocalDate endDate);
    List<MoviePerformanceDaily> findAllByMovieId(Long movieId);
}
