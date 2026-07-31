package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.DailyBusinessKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyBusinessKpiRepository extends JpaRepository<DailyBusinessKpi, Long> {
    Optional<DailyBusinessKpi> findByStatDate(LocalDate statDate);
    Optional<DailyBusinessKpi> findFirstByOrderByStatDateDesc();
    List<DailyBusinessKpi> findAllByStatDateBetweenOrderByStatDateAsc(LocalDate startDate, LocalDate endDate);
}
