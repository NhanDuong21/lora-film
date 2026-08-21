package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionAutomationRunRepository
        extends JpaRepository<PromotionAutomationRun, Long> {
    Optional<PromotionAutomationRun> findByPublicId(String publicId);
    Optional<PromotionAutomationRun> findByIdempotencyKey(String idempotencyKey);
    Optional<PromotionAutomationRun> findByPlaybookCodeAndTriggerReference(
            String playbookCode, String triggerReference);
    List<PromotionAutomationRun> findTop20ByOrderByCreatedAtDesc();
    List<PromotionAutomationRun> findByPlaybookPublicIdOrderByCreatedAtDesc(
            String playbookPublicId);
    List<PromotionAutomationRun> findByCampaignPublicIdOrderByCreatedAtDesc(
            String campaignPublicId);
}
