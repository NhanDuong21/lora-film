package com.project.analyticsservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = "SELECT m.movie_id as movieId, " +
           "COALESCE(r.movie_title, MAX(m.movie_title)) as movieTitle, " +
           "SUM(m.tickets_sold) as totalTicketsSold, " +
           "SUM(m.revenue) as totalRevenue, " +
           "MAX(m.updated_at) as lastUpdatedAt " +
           "FROM movie_daily_revenue_stats m " +
           "LEFT JOIN movie_revenue_stats r ON m.movie_id = r.movie_id " +
           "WHERE m.stat_date BETWEEN :startDate AND :endDate " +
           "AND (:movieId IS NULL OR m.movie_id = :movieId) " +
           "AND (:movieTitle IS NULL OR LOWER(m.movie_title) LIKE LOWER(CONCAT('%', :movieTitle, '%'))) " +
           "GROUP BY m.movie_id",
           countQuery = "SELECT COUNT(DISTINCT m.movie_id) " +
                        "FROM movie_daily_revenue_stats m " +
                        "WHERE m.stat_date BETWEEN :startDate AND :endDate " +
                        "AND (:movieId IS NULL OR m.movie_id = :movieId) " +
                        "AND (:movieTitle IS NULL OR LOWER(m.movie_title) LIKE LOWER(CONCAT('%', :movieTitle, '%')))",
           nativeQuery = true)
    Page<MovieDateRangeAggregateProjection> aggregateMovieRevenueForDateRangeWithFilters(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("movieId") Long movieId,
            @Param("movieTitle") String movieTitle,
            Pageable pageable);

    @Query(value = "SELECT m.movie_id as movieId, " +
           "COALESCE(r.movie_title, MAX(m.movie_title)) as movieTitle, " +
           "SUM(m.tickets_sold) as totalTicketsSold, " +
           "SUM(m.revenue) as totalRevenue, " +
           "MAX(m.updated_at) as lastUpdatedAt " +
           "FROM movie_daily_revenue_stats m " +
           "LEFT JOIN movie_revenue_stats r ON m.movie_id = r.movie_id " +
           "WHERE m.movie_id = :movieId AND m.stat_date BETWEEN :startDate AND :endDate " +
           "GROUP BY m.movie_id",
           nativeQuery = true)
    Optional<MovieDateRangeAggregateProjection> aggregateMovieRevenueForDateRangeAndMovieId(
            @Param("movieId") Long movieId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
