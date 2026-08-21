package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionRedemption;
import com.project.promotionservice.promotion.enums.PromotionRedemptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.math.BigDecimal;

public interface PromotionRedemptionRepository
        extends JpaRepository<PromotionRedemption, Long>,
        JpaSpecificationExecutor<PromotionRedemption> {

    List<PromotionRedemption> findByReservationPublicIdAndDeletedAtIsNull(
            String reservationPublicId);

    List<PromotionRedemption> findTop100ByUserPublicIdAndStatusInAndDeletedAtIsNullOrderByUpdatedAtDesc(
            String userPublicId, Collection<PromotionRedemptionStatus> statuses);

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

    List<PromotionRedemption> findByUserPromotionPublicIdInAndStatusInAndDeletedAtIsNull(
            Collection<String> userPromotionPublicIds,
            Collection<PromotionRedemptionStatus> statuses);

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

    @Query("""
            select coalesce(sum(redemption.discountAmount), 0)
            from PromotionRedemption redemption, PromotionReservation reservation
            where redemption.reservationPublicId = reservation.publicId
              and redemption.campaignPublicId = :campaignPublicId
              and redemption.status = com.project.promotionservice.promotion.enums.PromotionRedemptionStatus.RESERVED
              and reservation.status = com.project.promotionservice.reservation.enums.ReservationStatus.ACTIVE
              and redemption.deletedAt is null
              and reservation.deletedAt is null
            """)
    BigDecimal sumActiveReservedDiscountByCampaign(
            @Param("campaignPublicId") String campaignPublicId);

    @Query("""
            select (count(redemption) > 0)
            from PromotionRedemption redemption
            where redemption.campaignPublicId = :campaignPublicId
              and redemption.status = com.project.promotionservice.promotion.enums.PromotionRedemptionStatus.CONFIRMED
              and redemption.reservationPublicId <> :reservationPublicId
              and redemption.deletedAt is null
              and ((:bookingPublicId is not null
                    and redemption.bookingPublicId = :bookingPublicId)
                or (:orderPublicId is not null
                    and redemption.orderPublicId = :orderPublicId))
            """)
    boolean existsConfirmedCampaignConsumptionForBusinessKey(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("reservationPublicId") String reservationPublicId,
            @Param("bookingPublicId") String bookingPublicId,
            @Param("orderPublicId") String orderPublicId);

}
