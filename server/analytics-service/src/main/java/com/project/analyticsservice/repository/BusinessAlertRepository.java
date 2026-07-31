package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.BusinessAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface BusinessAlertRepository extends JpaRepository<BusinessAlert, Long> {
    Optional<BusinessAlert> findByInsightId(Long insightId);
    List<BusinessAlert> findAllByOrderByCreatedAtDesc();
    List<BusinessAlert> findTop100ByInsightIdInOrderByCreatedAtDesc(Collection<Long> insightIds);
}
