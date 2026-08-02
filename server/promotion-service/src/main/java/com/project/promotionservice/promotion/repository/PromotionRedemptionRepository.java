package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
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

    boolean existsByPromotionPublicIdAndDeletedAtIsNull(String promotionPublicId);

    boolean existsByCampaignPublicIdAndDeletedAtIsNull(String campaignPublicId);

    @Query("""
            select count(distinct coalesce(r.reservationPublicId, r.publicId))
            from PromotionRedemption r
            where r.campaignPublicId = :campaignPublicId
              and r.status in :statuses
              and r.deletedAt is null
            """)
    long countCampaignRedemptions(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("statuses") Collection<PromotionRedemptionStatus> statuses);

}
