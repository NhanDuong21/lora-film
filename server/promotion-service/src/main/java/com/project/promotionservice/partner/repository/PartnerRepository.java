package com.project.promotionservice.partner.repository;

import com.project.promotionservice.partner.entity.Partner;
import com.project.promotionservice.partner.enums.PartnerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long>,
        JpaSpecificationExecutor<Partner> {
    Optional<Partner> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Partner> findByPublicId(String publicId);
    Optional<Partner> findByCodeAndDeletedAtIsNull(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndDeletedAtIsNull(String code);
    Page<Partner> findByDeletedAtIsNull(Pageable pageable);
    long countByStatusAndDeletedAtIsNull(PartnerStatus status);
}
