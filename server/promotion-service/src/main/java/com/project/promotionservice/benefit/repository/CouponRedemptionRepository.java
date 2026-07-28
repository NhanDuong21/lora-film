package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.CouponRedemption;
import com.project.promotionservice.benefit.enums.BenefitEnums.RedemptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long>,
        JpaSpecificationExecutor<CouponRedemption> {

    Optional<CouponRedemption> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<CouponRedemption> findByReservationPublicIdAndDeletedAtIsNull(String reservationPublicId);

    long countByCouponPublicIdAndUserPublicIdAndStatusIn(
            String couponPublicId, String userPublicId, Collection<RedemptionStatus> statuses);

    long countByCouponPublicIdAndCustomerPhoneAndStatusIn(
            String couponPublicId, String customerPhone, Collection<RedemptionStatus> statuses);

    long countByCampaignPublicIdAndUserPublicIdAndStatusIn(
            String campaignPublicId, String userPublicId, Collection<RedemptionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CouponRedemption r where r.publicId = :publicId and r.deletedAt is null")
    Optional<CouponRedemption> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select r from CouponRedemption r
            where r.couponPublicId = :couponPublicId
              and r.userPublicId = :userPublicId
              and r.status in :statuses
              and ((:orderPublicId is not null and r.orderPublicId = :orderPublicId)
                or (:bookingPublicId is not null and r.bookingPublicId = :bookingPublicId)
                or (:paymentPublicId is not null and r.paymentPublicId = :paymentPublicId))
            """)
    Optional<CouponRedemption> findProcessedTransaction(
            @Param("couponPublicId") String couponPublicId,
            @Param("userPublicId") String userPublicId,
            @Param("orderPublicId") String orderPublicId,
            @Param("bookingPublicId") String bookingPublicId,
            @Param("paymentPublicId") String paymentPublicId,
            @Param("statuses") Collection<RedemptionStatus> statuses);
}
