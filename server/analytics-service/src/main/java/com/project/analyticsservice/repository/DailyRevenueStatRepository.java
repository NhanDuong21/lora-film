package com.project.analyticsservice.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.analyticsservice.dto.DailyRevenueSummaryProjection;
import com.project.analyticsservice.entity.DailyRevenueStat;

@Repository
public interface DailyRevenueStatRepository extends JpaRepository<DailyRevenueStat, Long> {

    Optional<DailyRevenueStat> findByStatDate(LocalDate statDate);

    List<DailyRevenueStat> findAllByStatDateBetweenOrderByStatDateAsc(LocalDate startDate, LocalDate endDate);

    List<DailyRevenueStat> findAllByStatDateBetweenOrderByStatDateDesc(LocalDate startDate, LocalDate endDate);

    boolean existsByStatDate(LocalDate statDate);

    @Query("SELECT " +
           "COALESCE(SUM(d.totalRevenue), 0) as totalRevenue, " +
           "COALESCE(SUM(d.totalBookingsCount), 0) as totalBookingsCount, " +
           "COALESCE(SUM(d.cancelledBookingsCount), 0) as cancelledBookingsCount, " +
           "COALESCE(SUM(d.totalTicketsSold), 0) as totalTicketsSold, " +
           "MAX(d.updatedAt) as lastUpdatedAt " +
           "FROM DailyRevenueStat d " +
           "WHERE d.statDate BETWEEN :startDate AND :endDate")
    DailyRevenueSummaryProjection aggregateRevenueSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
