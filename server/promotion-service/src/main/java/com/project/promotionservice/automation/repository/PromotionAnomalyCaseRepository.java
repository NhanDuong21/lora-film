package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionAnomalyCase;
import com.project.promotionservice.automation.enums.AnomalyCaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromotionAnomalyCaseRepository
        extends JpaRepository<PromotionAnomalyCase, Long> {
    Optional<PromotionAnomalyCase> findByPublicId(String publicId);
    Optional<PromotionAnomalyCase> findByAudienceMemberPublicId(
            String audienceMemberPublicId);
    List<PromotionAnomalyCase> findByStatusInAndTestDataOrderByCreatedAtDesc(
            Collection<AnomalyCaseStatus> statuses, Boolean testData);
    List<PromotionAnomalyCase> findByStatusInOrderByCreatedAtDesc(
            Collection<AnomalyCaseStatus> statuses);
    long countByRunPublicIdAndStatusIn(
            String runPublicId, Collection<AnomalyCaseStatus> statuses);
}
