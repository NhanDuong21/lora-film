package com.project.promotionservice.partner.repository;

import com.project.promotionservice.partner.entity.PartnerSettlement;
import com.project.promotionservice.partner.enums.SettlementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PartnerSettlementRepository extends JpaRepository<PartnerSettlement, Long>,
        JpaSpecificationExecutor<PartnerSettlement> {
    Optional<PartnerSettlement> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<PartnerSettlement> findBySettlementCodeAndDeletedAtIsNull(String code);
    boolean existsBySettlementCode(String code);
    Page<PartnerSettlement> findByDeletedAtIsNull(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PartnerSettlement s where s.publicId = :publicId and s.deletedAt is null")
    Optional<PartnerSettlement> findByPublicIdForUpdate(@Param("publicId") String publicId);

    boolean existsByPartnerPublicIdAndSettlementPeriodFromLessThanAndSettlementPeriodToGreaterThanAndDeletedAtIsNull(
            String partnerPublicId, Instant periodTo, Instant periodFrom);

    long countByStatusAndDeletedAtIsNull(SettlementStatus status);
}
