package com.project.promotionservice.repository;

import com.project.promotionservice.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findByPromotionCode(String code);

    boolean existsByPromotionCode(String code);

    Optional<Promotion> findByPromotionCodeIgnoreCase(String code);

    Page<Promotion> findByCampaignId(Long campaignId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1, p.version = p.version + 1 " +
           "WHERE p.id = :promotionId AND p.usedCount < p.usageLimit")
    int incrementUsedCountIfAvailable(@Param("promotionId") Long promotionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount - 1, p.version = p.version + 1 " +
           "WHERE p.id = :promotionId AND p.usedCount > 0")
    int decrementUsedCountIfPositive(@Param("promotionId") Long promotionId);
}
