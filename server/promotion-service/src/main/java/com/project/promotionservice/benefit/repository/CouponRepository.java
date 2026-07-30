package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.Instant;
import java.util.Collection;
import com.project.promotionservice.benefit.enums.BenefitEnums.CouponStatus;
import org.springframework.data.domain.Pageable;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<Coupon> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCase(String code);

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
