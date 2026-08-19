package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionRedemptionAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface PromotionRedemptionAdjustmentRepository
        extends JpaRepository<PromotionRedemptionAdjustment, Long>,
        JpaSpecificationExecutor<PromotionRedemptionAdjustment> {

    @Query("""
            select count(distinct adjustment.reservationPublicId)
            from PromotionRedemptionAdjustment adjustment
            where adjustment.adjustmentType = :type
              and adjustment.deletedAt is null
            """)
    long countDistinctReservationsByType(@Param("type") String type);

    @Query("""
            select count(distinct adjustment.reservationPublicId)
            from PromotionRedemptionAdjustment adjustment
            where adjustment.adjustmentType = :type
              and adjustment.occurredAt >= :from
              and adjustment.deletedAt is null
            """)
    long countDistinctReservationsByTypeSince(
            @Param("type") String type,
            @Param("from") Instant from);
}
