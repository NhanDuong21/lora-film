package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, Long>, JpaSpecificationExecutor<PromotionCampaign> {
    Optional<PromotionCampaign> findByPublicId(String publicId);
    Optional<PromotionCampaign> findByCode(String code);
    Optional<PromotionCampaign> findBySlug(String slug);
    boolean existsByCodeAndDeletedAtIsNull(String code);
    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
