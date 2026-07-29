package com.project.promotionservice.benefit.repository;

import com.project.promotionservice.benefit.entity.Voucher;
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
import com.project.promotionservice.benefit.enums.BenefitEnums.VoucherStatus;
import org.springframework.data.domain.Pageable;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long>, JpaSpecificationExecutor<Voucher> {

    Optional<Voucher> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<Voucher> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where upper(v.code) = upper(:code) and v.deletedAt is null")
    Optional<Voucher> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.publicId = :publicId and v.deletedAt is null")
    Optional<Voucher> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select v.publicId from Voucher v
            where v.status in :statuses and v.validTo <= :now and v.deletedAt is null
            order by v.validTo asc
            """)
    java.util.List<String> findExpirableIds(@Param("statuses") Collection<VoucherStatus> statuses,
                                            @Param("now") Instant now, Pageable pageable);
}
