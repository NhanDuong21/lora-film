package com.project.promotionservice.reservation.repository;

import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionReservationRepository extends
        JpaRepository<PromotionReservation, Long>,
        JpaSpecificationExecutor<PromotionReservation> {

    Optional<PromotionReservation> findByPublicIdAndDeletedAtIsNull(String publicId);

    Optional<PromotionReservation> findByReservationCodeAndDeletedAtIsNull(String reservationCode);

    Optional<PromotionReservation> findByReservationScopeKeyAndDeletedAtIsNull(
            String reservationScopeKey);

    @Query("""
            select count(distinct reservation.id)
            from PromotionReservation reservation,
                 PromotionRedemption redemption
            where redemption.reservationPublicId = reservation.publicId
              and redemption.campaignPublicId = :campaignPublicId
              and reservation.status = :status
              and reservation.deletedAt is null
              and redemption.deletedAt is null
            """)
    long countByCampaignAndStatus(
            @Param("campaignPublicId") String campaignPublicId,
            @Param("status") ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation from PromotionReservation reservation
            where reservation.publicId = :publicId and reservation.deletedAt is null
            """)
    Optional<PromotionReservation> findByPublicIdForUpdate(@Param("publicId") String publicId);

    List<PromotionReservation> findByStatusAndReservationExpiredAtLessThanEqualAndDeletedAtIsNull(
            ReservationStatus status, Instant now, Pageable pageable);

    @Query("""
            select reservation.publicId from PromotionReservation reservation
            where reservation.status = :status
              and reservation.reservationExpiredAt <= :now
              and reservation.deletedAt is null
              and (
                    reservation.expirationNextAttemptAt is null
                    or reservation.expirationNextAttemptAt <= :now
              )
            order by reservation.reservationExpiredAt asc
            """)
    List<String> findDueCandidateIds(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            select count(reservation) from PromotionReservation reservation
            where reservation.status = :status
              and reservation.reservationExpiredAt <= :now
              and reservation.deletedAt is null
            """)
    long countExpirationBacklog(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now);

    @Query("""
            select min(reservation.reservationExpiredAt)
            from PromotionReservation reservation
            where reservation.status = :status
              and reservation.reservationExpiredAt <= :now
              and reservation.deletedAt is null
            """)
    Optional<Instant> findOldestExpiredAt(
            @Param("status") ReservationStatus status,
            @Param("now") Instant now);
}
