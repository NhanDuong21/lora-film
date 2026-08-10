package com.lorafilm.movie.pricing.repository;

import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PricePolicyRepository extends JpaRepository<PricePolicy, Long> {

    @EntityGraph(attributePaths = {"cinema", "supersedesPolicy", "rules", "rules.seatType", "rules.auditorium"})
    Optional<PricePolicy> findByPublicIdAndDeletedAtIsNull(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cinema", "supersedesPolicy", "rules", "rules.seatType", "rules.auditorium"})
    @Query("select p from PricePolicy p where p.publicId = :publicId and p.deletedAt is null")
    Optional<PricePolicy> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @EntityGraph(attributePaths = {"cinema"})
    @Query("""
            select p from PricePolicy p
            where p.deletedAt is null
              and (:cinemaPublicId is null or p.cinema.publicId = :cinemaPublicId)
              and (:status is null or p.status = :status)
              and (:effectiveDate is null or
                   (p.effectiveFrom <= :effectiveDate and
                    (p.effectiveTo is null or p.effectiveTo >= :effectiveDate)))
            """)
    Page<PricePolicy> search(@Param("cinemaPublicId") String cinemaPublicId,
                            @Param("status") PricePolicyStatus status,
                            @Param("effectiveDate") LocalDate effectiveDate,
                            Pageable pageable);

    @EntityGraph(attributePaths = {"cinema"})
    @Query("""
            select p from PricePolicy p
            where p.deletedAt is null
              and p.status = com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus.ACTIVE
              and (:cinemaPublicId is null or p.cinema.publicId = :cinemaPublicId)
              and (:effectiveDate is null or
                   (p.effectiveFrom <= :effectiveDate and
                    (p.effectiveTo is null or p.effectiveTo >= :effectiveDate)))
            order by p.createdAt desc
            """)
    List<PricePolicy> findActiveDisplayCandidates(@Param("cinemaPublicId") String cinemaPublicId,
                                                  @Param("effectiveDate") LocalDate effectiveDate);

    @EntityGraph(attributePaths = {"cinema", "rules", "rules.seatType", "rules.auditorium"})
    @Query("""
            select distinct p from PricePolicy p
            where p.cinema.id = :cinemaId
              and p.status = com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus.ACTIVE
              and p.deletedAt is null
              and p.effectiveFrom <= :localDate
              and (p.effectiveTo is null or p.effectiveTo >= :localDate)
            """)
    List<PricePolicy> findEffectiveActivePolicies(@Param("cinemaId") Long cinemaId,
                                                  @Param("localDate") LocalDate localDate);

    @EntityGraph(attributePaths = {"cinema", "rules", "rules.seatType", "rules.auditorium"})
    @Query("""
            select distinct p from PricePolicy p
            where p.cinema.id = :cinemaId
              and p.status = com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus.ACTIVE
              and p.deletedAt is null
              and p.effectiveFrom <= :toInclusive
              and (p.effectiveTo is null or p.effectiveTo >= :fromInclusive)
            """)
    List<PricePolicy> findActivePoliciesOverlappingDateRange(
            @Param("cinemaId") Long cinemaId,
            @Param("fromInclusive") LocalDate fromInclusive,
            @Param("toInclusive") LocalDate toInclusive);

}
