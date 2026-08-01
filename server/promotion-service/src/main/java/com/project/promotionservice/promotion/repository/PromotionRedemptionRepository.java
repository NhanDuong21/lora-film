package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PromotionRedemptionRepository
        extends JpaRepository<PromotionRedemption, Long> {

    List<PromotionRedemption> findByReservationPublicIdAndDeletedAtIsNull(
            String reservationPublicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from PromotionRedemption r
            where r.reservationPublicId = :reservationPublicId
              and r.deletedAt is null
            order by r.id asc
            """)
    List<PromotionRedemption> findByReservationPublicIdForUpdate(
            @Param("reservationPublicId") String reservationPublicId);

    long countByPromotionPublicIdAndStatusInAndDeletedAtIsNull(
            String promotionPublicId, Collection<PromotionRedemptionStatus> statuses);

    long countByPromotionPublicIdAndUserPublicIdAndStatusInAndDeletedAtIsNull(
            String promotionPublicId, String userPublicId,
            Collection<PromotionRedemptionStatus> statuses);

    long countByUserPromotionPublicIdAndStatusInAndDeletedAtIsNull(
            String userPromotionPublicId, Collection<PromotionRedemptionStatus> statuses);

    @Query("""
            select count(r) from PromotionRedemption r, Promotion p
            where r.promotionPublicId = p.publicId
              and p.campaignPublicId = :campaignPublicId
              and r.status in :statuses
              and r.deletedAt is null
              and p.deletedAt is null
            """)
    long countCampaignRedemptions(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("statuses") Collection<PromotionRedemptionStatus> statuses);

    @Query("""
            select count(r) from PromotionRedemption r, Promotion p
            where r.promotionPublicId = p.publicId
              and p.campaignPublicId = :campaignPublicId
              and p.promotionType = :promotionType
              and r.status in :statuses
              and r.deletedAt is null
              and p.deletedAt is null
            """)
    long countCampaignRedemptionsByPromotionType(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("promotionType") PromotionType promotionType,
            @Param("statuses") Collection<PromotionRedemptionStatus> statuses);

    @Query("""
            select count(r) from PromotionRedemption r, Promotion p
            where r.promotionPublicId = p.publicId
              and p.campaignPublicId = :campaignPublicId
              and r.userPublicId = :userPublicId
              and r.status in :statuses
              and r.deletedAt is null
              and p.deletedAt is null
            """)
    long countCampaignUserRedemptions(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("userPublicId") String userPublicId,
            @Param("statuses") Collection<PromotionRedemptionStatus> statuses);

    @Query("""
            select count(r) from PromotionRedemption r, Promotion p
            where r.promotionPublicId = p.publicId
              and p.campaignPublicId = :campaignPublicId
              and p.promotionType = :promotionType
              and r.userPublicId = :userPublicId
              and r.status in :statuses
              and r.deletedAt is null
              and p.deletedAt is null
            """)
    long countCampaignUserRedemptionsByPromotionType(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("promotionType") PromotionType promotionType,
            @Param("userPublicId") String userPublicId,
            @Param("statuses") Collection<PromotionRedemptionStatus> statuses);
}
