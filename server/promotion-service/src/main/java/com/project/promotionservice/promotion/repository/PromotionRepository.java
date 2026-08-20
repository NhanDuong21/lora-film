package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.Promotion;
import com.project.promotionservice.promotion.enums.PromotionStatus;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.LegalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface PromotionRepository
        extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    Optional<Promotion> findByPublicIdAndDeletedAtIsNull(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Promotion p
            where p.publicId = :publicId and p.deletedAt is null
            """)
    Optional<Promotion> findByPublicIdForUpdate(@Param("publicId") String publicId);

    Optional<Promotion> findByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
            PromotionType promotionType, String code);

    boolean existsByPromotionTypeAndCodeIgnoreCaseAndDeletedAtIsNull(
            PromotionType promotionType, String code);

    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    boolean existsByCampaignPublicIdAndDeletedAtIsNull(String campaignPublicId);

    boolean existsByCampaignPublicIdAndPublicVisibleTrueAndDeletedAtIsNull(
            String campaignPublicId);

    List<Promotion> findByPublicIdInAndDeletedAtIsNull(Collection<String> publicIds);

    List<Promotion> findByCampaignPublicIdAndDeletedAtIsNullOrderByPriorityAsc(
            String campaignPublicId);

    @Query("""
            select p.publicId from Promotion p, PromotionCampaign c
            where p.campaignPublicId = c.publicId
              and p.status = :status
              and c.status = :campaignStatus
              and c.legalStatus = :legalStatus
              and p.validFrom <= :now
              and p.validTo > :now
              and p.deletedAt is null
              and c.deletedAt is null
            order by p.validFrom asc
            """)
    List<String> findActivatableIds(
            @Param("status") PromotionStatus status,
            @Param("campaignStatus") com.project.promotionservice.promotion.enums.CampaignStatus campaignStatus,
            @Param("legalStatus") com.project.promotionservice.promotion.enums.LegalStatus legalStatus,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select p.publicId from Promotion p
            where p.status in :statuses
              and p.validTo <= :now
              and p.deletedAt is null
            order by p.validTo asc
            """)
    List<String> findExpirableIds(
            @Param("statuses") Collection<PromotionStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
            update Promotion p
            set p.status = :activeStatus,
                p.updatedAt = :now,
                p.updatedBy = :actor
            where p.campaignPublicId = :campaignPublicId
              and p.status = :draftStatus
              and p.validFrom <= :now
              and p.validTo > :now
              and p.deletedAt is null
            """)
    int activateDraftPromotions(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("draftStatus") PromotionStatus draftStatus,
            @Param("activeStatus") PromotionStatus activeStatus,
            @Param("now") Instant now,
            @Param("actor") String actor);

    @Query("""
            select (count(p) > 0) from Promotion p
            where p.campaignPublicId = :campaignPublicId
              and p.status in :statuses
              and p.validFrom < :campaignEnd
              and p.validTo > :campaignStart
              and p.deletedAt is null
            """)
    boolean existsConfiguredForCampaign(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("statuses") Collection<PromotionStatus> statuses,
            @Param("campaignStart") Instant campaignStart,
            @Param("campaignEnd") Instant campaignEnd);

    @Query("""
            select p from Promotion p
            where p.promotionType = :type
              and p.status = :status
              and p.deletedAt is null
              and p.validFrom <= :now
              and p.validTo > :now
            order by p.priority asc, p.createdAt asc
            """)
    List<Promotion> findRuntimeCandidates(
            @Param("type") PromotionType type,
            @Param("status") PromotionStatus status,
            @Param("now") Instant now);

    @Query("""
            select p from Promotion p, PromotionCampaign c
            where p.campaignPublicId = c.publicId
              and p.promotionType = :type
              and p.status = :status
              and c.status = :campaignStatus
              and c.legalStatus = :legalStatus
              and p.publicVisible = true
              and p.deletedAt is null
              and c.deletedAt is null
              and p.validFrom <= :now
              and p.validTo > :now
              and c.startAt <= :now
              and c.endAt > :now
              and c.killSwitch = false
            """)
    Page<Promotion> findPublicPromotions(
            @Param("type") PromotionType type,
            @Param("status") PromotionStatus status,
            @Param("campaignStatus") com.project.promotionservice.promotion.enums.CampaignStatus campaignStatus,
            @Param("legalStatus") LegalStatus legalStatus,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select p from Promotion p, PromotionCampaign c
            where p.campaignPublicId = c.publicId
              and p.promotionType = :type
              and p.status = :status
              and c.status = :campaignStatus
              and c.legalStatus = :legalStatus
              and p.deletedAt is null
              and c.deletedAt is null
              and c.killSwitch = false
              and p.validFrom <= :now
              and p.validTo > :now
              and c.startAt <= :now
              and c.endAt > :now
            """)
    Page<Promotion> findSystemPromotions(
            @Param("type") PromotionType type,
            @Param("status") PromotionStatus status,
            @Param("campaignStatus") com.project.promotionservice.promotion.enums.CampaignStatus campaignStatus,
            @Param("legalStatus") LegalStatus legalStatus,
            @Param("now") Instant now,
            Pageable pageable);
}
