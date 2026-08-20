package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionAudienceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionAudienceSnapshotRepository
        extends JpaRepository<PromotionAudienceSnapshot, Long> {
    Optional<PromotionAudienceSnapshot> findByRunPublicId(String runPublicId);
}
