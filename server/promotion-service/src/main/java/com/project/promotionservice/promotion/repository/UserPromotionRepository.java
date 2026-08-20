package com.project.promotionservice.promotion.repository;

import com.project.promotionservice.promotion.entity.UserPromotion;
import com.project.promotionservice.promotion.enums.PromotionType;
import com.project.promotionservice.promotion.enums.UserPromotionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface UserPromotionRepository extends JpaRepository<UserPromotion, Long> {

    Optional<UserPromotion> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<UserPromotion> findByIssuanceKey(String issuanceKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserPromotion up where up.issuanceKey = :issuanceKey and up.deletedAt is null")
    Optional<UserPromotion> findByIssuanceKeyForUpdate(
            @Param("issuanceKey") String issuanceKey);

    Optional<UserPromotion> findFirstByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNullOrderByIdDesc(
            String userPublicId, String promotionPublicId);

    boolean existsByUserPublicIdAndPromotionPublicIdAndDeletedAtIsNull(
            String userPublicId, String promotionPublicId);

    long countByPromotionPublicIdAndDeletedAtIsNull(String promotionPublicId);

    List<UserPromotion> findTop100ByUserPublicIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
            String userPublicId);

    @Query("""
            select (count(up) > 0) from UserPromotion up, Promotion p
            where up.promotionPublicId = p.publicId
              and p.campaignPublicId = :campaignPublicId
              and up.deletedAt is null
            """)
    boolean existsByCampaignPublicId(
            @Param("campaignPublicId") String campaignPublicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select up from UserPromotion up
            where up.publicId = :publicId and up.deletedAt is null
            """)
    Optional<UserPromotion> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query("""
            select up from UserPromotion up, Promotion p
            where up.userPublicId = :userPublicId
              and (:status is null or up.status = :status)
              and p.publicId = up.promotionPublicId
              and p.promotionType <> :excludedPromotionType
              and up.deletedAt is null
            """)
    Page<UserPromotion> findWallet(
            @Param("userPublicId") String userPublicId,
            @Param("status") UserPromotionStatus status,
            @Param("excludedPromotionType") PromotionType excludedPromotionType,
            Pageable pageable);

    @Query("""
            select up from UserPromotion up, Promotion p
            where up.userPublicId = :userPublicId
              and up.status = :status
              and up.usageCount < up.maxUsage
              and up.validFrom <= :now
              and up.validTo > :now
              and p.publicId = up.promotionPublicId
              and p.promotionType <> :excludedPromotionType
              and up.deletedAt is null
              and p.deletedAt is null
            """)
    Page<UserPromotion> findUsableWallet(
            @Param("userPublicId") String userPublicId,
            @Param("status") UserPromotionStatus status,
            @Param("excludedPromotionType") PromotionType excludedPromotionType,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select up from UserPromotion up
            where up.userPublicId = :userPublicId
              and up.status = :status
              and up.usageCount < up.maxUsage
              and up.validFrom <= :now
              and up.validTo > :now
              and up.deletedAt is null
            """)
    List<UserPromotion> findAvailableWalletItems(
            @Param("userPublicId") String userPublicId,
            @Param("status") UserPromotionStatus status,
            @Param("now") Instant now);

    @Query("""
            select up.publicId from UserPromotion up
            where up.status in :statuses
              and up.validTo <= :now
              and up.deletedAt is null
            order by up.validTo asc
            """)
    List<String> findExpirableIds(
            @Param("statuses") Collection<UserPromotionStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
