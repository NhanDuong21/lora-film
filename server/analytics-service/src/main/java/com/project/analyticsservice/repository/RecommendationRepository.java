package com.project.analyticsservice.repository;

import com.project.analyticsservice.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findByInsightIdAndActionType(Long insightId, String actionType);
    List<Recommendation> findAllByOrderByCreatedAtDesc();
    List<Recommendation> findTop100ByInsightIdInOrderByCreatedAtDesc(Collection<Long> insightIds);
}
