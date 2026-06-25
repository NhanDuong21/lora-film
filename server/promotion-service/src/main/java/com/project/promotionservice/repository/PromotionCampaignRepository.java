package com.project.promotionservice.repository;

import com.project.promotionservice.entity.PromotionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, Long>, JpaSpecificationExecutor<PromotionCampaign> {
}
