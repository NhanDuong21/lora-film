package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.CustomerSegmentDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CustomerSegmentDailyRepository extends JpaRepository<CustomerSegmentDaily, Long> {
    Optional<CustomerSegmentDaily> findByMembershipTierAndStatDate(String membershipTier, LocalDate statDate);
    List<CustomerSegmentDaily> findAllByStatDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<CustomerSegmentDaily> findFirstByStatDateLessThanEqualOrderByStatDateDesc(LocalDate statDate);
    List<CustomerSegmentDaily> findAllByStatDate(LocalDate statDate);
}
