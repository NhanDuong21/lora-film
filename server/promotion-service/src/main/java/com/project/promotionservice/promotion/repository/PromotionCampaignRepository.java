package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.PromotionCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, Long>, JpaSpecificationExecutor<PromotionCampaign> {
    Optional<PromotionCampaign> findByPublicId(String publicId);
    Optional<PromotionCampaign> findByCode(String code);
    Optional<PromotionCampaign> findBySlug(String slug);
    boolean existsByCodeAndDeletedAtIsNull(String code);
    boolean existsBySlugAndDeletedAtIsNull(String slug);
    boolean existsByPartnerPublicIdAndDeletedAtIsNull(String partnerPublicId);
    List<PromotionCampaign> findByPartnerPublicIdAndDeletedAtIsNull(String partnerPublicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PromotionCampaign c where c.publicId = :publicId and c.deletedAt is null")
    Optional<PromotionCampaign> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select c.publicId from PromotionCampaign c
            where c.status in :statuses and c.endAt <= :now and c.deletedAt is null
            order by c.endAt asc
            """)
    List<String> findExpiredIds(@Param("statuses") java.util.Collection<com.project.promotionservice.promotion.enums.CampaignStatus> statuses,
                                @Param("now") java.time.Instant now, Pageable pageable);

    @Query("""
            select c.publicId from PromotionCampaign c
            where c.status = :status
              and c.autoActivate = true
              and c.startAt <= :now
              and c.endAt > :now
              and c.deletedAt is null
            order by c.startAt asc
            """)
    List<String> findActivatableIds(
            @Param("status") com.project.promotionservice.promotion.enums.CampaignStatus status,
            @Param("now") java.time.Instant now,
            Pageable pageable);
}
