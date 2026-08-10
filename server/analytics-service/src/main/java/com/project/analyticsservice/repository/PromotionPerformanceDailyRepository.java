package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.PromotionPerformanceDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PromotionPerformanceDailyRepository extends JpaRepository<PromotionPerformanceDaily, Long> {
    Optional<PromotionPerformanceDaily> findByPromotionKeyAndStatDate(String promotionKey, LocalDate statDate);
    List<PromotionPerformanceDaily> findAllByStatDateBetween(LocalDate startDate, LocalDate endDate);
}
