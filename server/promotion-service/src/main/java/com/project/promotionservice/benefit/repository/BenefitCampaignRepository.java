package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.promotion.entity.PromotionCampaign;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BenefitCampaignRepository extends JpaRepository<PromotionCampaign, Long> {

    Optional<PromotionCampaign> findByPublicIdAndDeletedAtIsNull(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select campaign from PromotionCampaign campaign
            where campaign.publicId = :publicId and campaign.deletedAt is null
            """)
    Optional<PromotionCampaign> findByPublicIdForUpdate(@Param("publicId") String publicId);
}
