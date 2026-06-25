package com.project.promotionservice.repository;

import com.project.promotionservice.entity.PromotionUsage;
import com.project.promotionservice.enums.PromotionUsageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long>, JpaSpecificationExecutor<PromotionUsage> {

    Optional<PromotionUsage> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    long countByPromotionIdAndUserIdAndStatusIn(
            Long promotionId,
            Long userId,
            Collection<PromotionUsageStatus> statuses
    );

    Page<PromotionUsage> findByStatusAndExpiresAtBeforeOrderByExpiresAtAscIdAsc(
            PromotionUsageStatus status,
            LocalDateTime now,
            Pageable pageable
    );

    Page<PromotionUsage> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<PromotionUsage> findByUserIdAndStatus(
            Long userId,
            PromotionUsageStatus status,
            Pageable pageable
    );

    Page<PromotionUsage> findByPromotionId(
            Long promotionId,
            Pageable pageable
    );

    Page<PromotionUsage> findByPromotionIdAndStatus(
            Long promotionId,
            PromotionUsageStatus status,
            Pageable pageable
    );
}
