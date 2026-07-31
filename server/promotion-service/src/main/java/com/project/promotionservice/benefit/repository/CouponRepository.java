package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.time.Instant;
import java.util.Collection;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import com.project.promotionservice.promotion.enums.CampaignStatus;
import org.springframework.data.domain.Pageable;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<Coupon> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
            select case when count(c) > 0 then true else false end
            from Coupon c
            where c.campaignPublicId = :campaignPublicId
              and c.status in :statuses
              and c.validFrom < :campaignEnd
              and c.validTo > :campaignStart
              and c.deletedAt is null
            """)
    boolean existsConfiguredForCampaign(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("statuses") Collection<CouponStatus> statuses,
            @Param("campaignStart") Instant campaignStart,
            @Param("campaignEnd") Instant campaignEnd);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            update Coupon c
            set c.status = :activeStatus, c.updatedBy = :actor
            where c.campaignPublicId = :campaignPublicId
              and c.status = :draftStatus
              and c.validFrom <= :now
              and c.validTo > :now
              and c.deletedAt is null
            """)
    int activateDraftCoupons(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("draftStatus") CouponStatus draftStatus,
            @Param("activeStatus") CouponStatus activeStatus,
            @Param("now") Instant now,
            @Param("actor") String actor);

    @Query("""
            select c.publicId from Coupon c, PromotionCampaign p
            where c.status = :status
              and c.validFrom <= :now
              and c.validTo > :now
              and c.deletedAt is null
              and p.publicId = c.campaignPublicId
              and p.status = :campaignStatus
              and p.deletedAt is null
            order by c.validFrom asc
            """)
    java.util.List<String> findActivatableIds(
            @Param("status") CouponStatus status,
            @Param("campaignStatus") CampaignStatus campaignStatus,
            @Param("now") Instant now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where upper(c.code) = upper(:code) and c.deletedAt is null")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.publicId = :publicId and c.deletedAt is null")
    Optional<Coupon> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select c.publicId from Coupon c
            where c.status in :statuses and c.validTo <= :now and c.deletedAt is null
            order by c.validTo asc
            """)
    java.util.List<String> findExpirableIds(@Param("statuses") Collection<CouponStatus> statuses,
                                            @Param("now") Instant now, Pageable pageable);
}
