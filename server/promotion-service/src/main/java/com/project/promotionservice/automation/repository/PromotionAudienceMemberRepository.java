package com.project.promotionservice.automation.repository;

import com.project.promotionservice.automation.entity.PromotionAudienceMember;
import com.project.promotionservice.automation.enums.AudienceMemberStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;

public interface PromotionAudienceMemberRepository
        extends JpaRepository<PromotionAudienceMember, Long> {
    List<PromotionAudienceMember> findByRunPublicIdAndStatusOrderByIdAsc(
            String runPublicId, AudienceMemberStatus status, Pageable pageable);
    List<PromotionAudienceMember> findByRunPublicIdAndStatusAndAttemptCountLessThanOrderByIdAsc(
            String runPublicId, AudienceMemberStatus status, Integer attemptCount,
            Pageable pageable);
    long countByRunPublicIdAndStatus(String runPublicId, AudienceMemberStatus status);
    java.util.Optional<PromotionAudienceMember> findFirstByRunPublicIdOrderByIdAsc(
            String runPublicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from PromotionAudienceMember m where m.publicId = :publicId and m.deletedAt is null")
    java.util.Optional<PromotionAudienceMember> findByPublicIdForUpdate(
            @Param("publicId") String publicId);
}
