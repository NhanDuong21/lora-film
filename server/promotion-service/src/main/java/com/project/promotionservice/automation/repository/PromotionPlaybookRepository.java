package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionPlaybook;
import com.project.promotionservice.automation.enums.PlaybookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PromotionPlaybookRepository extends JpaRepository<PromotionPlaybook, Long> {
    Optional<PromotionPlaybook> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<PromotionPlaybook> findByCodeAndDeletedAtIsNull(String code);
    Optional<PromotionPlaybook> findByCodeAndStatusAndDeletedAtIsNull(
            String code, PlaybookStatus status);
    List<PromotionPlaybook> findAllByDeletedAtIsNullOrderByCodeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PromotionPlaybook p where p.publicId = :publicId and p.deletedAt is null")
    Optional<PromotionPlaybook> findByPublicIdForUpdate(@Param("publicId") String publicId);
}
