package com.project.analyticsservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.analyticsservice.dto.MovieDateRangeAggregateProjection;
import com.project.analyticsservice.entity.MovieDailyRevenueStat;

@Repository
public interface MovieDailyRevenueStatRepository extends JpaRepository<MovieDailyRevenueStat, Long> {

    Optional<MovieDailyRevenueStat> findByMovieIdAndStatDate(Long movieId, LocalDate statDate);

    List<MovieDailyRevenueStat> findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(Long movieId, LocalDate startDate, LocalDate endDate);

    List<MovieDailyRevenueStat> findAllByStatDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m.movieId as movieId, " +
           "MIN(m.movieTitle) as movieTitle, " +
           "SUM(m.ticketsSold) as totalTicketsSold, " +
           "SUM(m.revenue) as totalRevenue, " +
           "MAX(m.updatedAt) as lastUpdatedAt " +
           "FROM MovieDailyRevenueStat m " +
           "WHERE m.statDate BETWEEN :startDate AND :endDate " +
           "GROUP BY m.movieId")
    List<MovieDateRangeAggregateProjection> aggregateMovieRevenueForDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
