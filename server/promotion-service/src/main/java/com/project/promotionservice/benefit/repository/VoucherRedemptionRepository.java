package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.VoucherRedemption;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, Long>,
        JpaSpecificationExecutor<VoucherRedemption> {

    Optional<VoucherRedemption> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<VoucherRedemption> findByReservationPublicIdAndDeletedAtIsNull(String reservationPublicId);

    boolean existsByOrderPublicIdAndStatusIn(
            String orderPublicId, Collection<RedemptionStatus> statuses);

    long countByCampaignPublicIdAndRedeemedByAndStatusIn(
            String campaignPublicId, String redeemedBy, Collection<RedemptionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VoucherRedemption r where r.publicId = :publicId and r.deletedAt is null")
    Optional<VoucherRedemption> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select r from VoucherRedemption r
            where r.voucherPublicId = :voucherPublicId
              and r.redeemedBy = :redeemedBy
              and r.status in :statuses
              and ((:orderPublicId is not null and r.orderPublicId = :orderPublicId)
                or (:bookingPublicId is not null and r.bookingPublicId = :bookingPublicId)
                or (:paymentPublicId is not null and r.paymentPublicId = :paymentPublicId))
            """)
    Optional<VoucherRedemption> findProcessedTransaction(
            @Param("voucherPublicId") String voucherPublicId,
            @Param("redeemedBy") String redeemedBy,
            @Param("orderPublicId") String orderPublicId,
            @Param("bookingPublicId") String bookingPublicId,
            @Param("paymentPublicId") String paymentPublicId,
            @Param("statuses") Collection<RedemptionStatus> statuses);

    @Query("""
            select coalesce(sum(r.discountAmount), 0) from VoucherRedemption r
            where r.campaignPublicId = :campaignPublicId
              and r.status in :statuses
              and r.confirmedAt >= :from and r.confirmedAt < :to
              and r.deletedAt is null
            """)
    BigDecimal sumConfirmedDiscount(@Param("campaignPublicId") String campaignPublicId,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    @Param("statuses") Collection<RedemptionStatus> statuses);

    @Query("""
            select count(r) from VoucherRedemption r
            where r.campaignPublicId = :campaignPublicId
              and r.status in :statuses
              and r.confirmedAt >= :from and r.confirmedAt < :to
              and r.deletedAt is null
            """)
    long countConfirmed(@Param("campaignPublicId") String campaignPublicId,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        @Param("statuses") Collection<RedemptionStatus> statuses);

    @Query("""
            select coalesce(sum(r.discountAmount), 0) from VoucherRedemption r
            where r.campaignPublicId in
                (select c.publicId from PromotionCampaign c
                 where c.partnerPublicId = :partnerPublicId and c.deletedAt is null)
              and r.status in :statuses
              and r.confirmedAt >= :from and r.confirmedAt < :to
              and r.deletedAt is null
            """)
    BigDecimal sumConfirmedDiscountForPartner(@Param("partnerPublicId") String partnerPublicId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to,
                                               @Param("statuses") Collection<RedemptionStatus> statuses);

    @Query("""
            select count(r) from VoucherRedemption r
            where r.campaignPublicId in
                (select c.publicId from PromotionCampaign c
                 where c.partnerPublicId = :partnerPublicId and c.deletedAt is null)
              and r.status in :statuses
              and r.confirmedAt >= :from and r.confirmedAt < :to
              and r.deletedAt is null
            """)
    long countConfirmedForPartner(@Param("partnerPublicId") String partnerPublicId,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to,
                                  @Param("statuses") Collection<RedemptionStatus> statuses);
}
