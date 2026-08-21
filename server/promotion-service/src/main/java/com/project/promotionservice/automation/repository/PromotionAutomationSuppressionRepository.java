package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionAutomationSuppression;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionAutomationSuppressionRepository
        extends JpaRepository<PromotionAutomationSuppression, Long> {
    boolean existsByPlaybookCodeAndTriggerReference(
            String playbookCode, String triggerReference);
    Optional<PromotionAutomationSuppression> findByPlaybookCodeAndTriggerReference(
            String playbookCode, String triggerReference);
}
