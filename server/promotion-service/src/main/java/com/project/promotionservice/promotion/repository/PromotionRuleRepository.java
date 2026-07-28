package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRuleRepository extends JpaRepository<PromotionRule, Long>, JpaSpecificationExecutor<PromotionRule> {
    Optional<PromotionRule> findByPublicId(String publicId);
    Optional<PromotionRule> findByCode(String code);
    List<PromotionRule> findByCampaignPublicIdAndDeletedAtIsNull(String campaignPublicId);
    boolean existsByCodeAndCampaignPublicIdAndDeletedAtIsNull(String code, String campaignPublicId);
}
