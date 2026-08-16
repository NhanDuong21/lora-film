package com.project.paymentservice.repository;

import com.project.paymentservice.entity.CounterCashSession;
import com.project.paymentservice.enumtype.CounterCashSessionStatus;
import com.project.paymentservice.enumtype.CashVerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CounterCashSessionRepository extends JpaRepository<CounterCashSession, Long> {
    Optional<CounterCashSession> findFirstByEmployeeAccountIdAndStatusOrderByOpenedAtDesc(
            Long employeeAccountId, CounterCashSessionStatus status);

    List<CounterCashSession> findTop10ByEmployeeAccountIdOrderByOpenedAtDesc(Long employeeAccountId);

    Page<CounterCashSession> findByStatusAndVerificationStatus(
            CounterCashSessionStatus status, CashVerificationStatus verificationStatus, Pageable pageable);
    Page<CounterCashSession> findByStatus(CounterCashSessionStatus status, Pageable pageable);
    Page<CounterCashSession> findByStatusAndCinemaPublicId(
            CounterCashSessionStatus status, String cinemaPublicId, Pageable pageable);
    Page<CounterCashSession> findByStatusAndCinemaPublicIdAndVerificationStatus(
            CounterCashSessionStatus status, String cinemaPublicId,
            CashVerificationStatus verificationStatus, Pageable pageable);
    long countByStatusAndVerificationStatusIn(
            CounterCashSessionStatus status, List<CashVerificationStatus> verificationStatuses);
    long countByStatusAndCinemaPublicIdAndVerificationStatusIn(
            CounterCashSessionStatus status, String cinemaPublicId,
            List<CashVerificationStatus> verificationStatuses);
    long countByStatusAndVerificationStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
            CounterCashSessionStatus status, List<CashVerificationStatus> verificationStatuses,
            Instant periodStart, Instant periodEndExclusive);
    long countByStatusAndCinemaPublicIdAndVerificationStatusInAndClosedAtGreaterThanEqualAndClosedAtLessThan(
            CounterCashSessionStatus status, String cinemaPublicId,
            List<CashVerificationStatus> verificationStatuses,
            Instant periodStart, Instant periodEndExclusive);

    @Query("select coalesce(sum(abs(s.varianceAmount)), 0) from CounterCashSession s "
            + "where s.status = :status and s.verificationStatus in :verificationStatuses")
    java.math.BigDecimal sumAbsoluteVariance(
            @Param("status") CounterCashSessionStatus status,
            @Param("verificationStatuses") List<CashVerificationStatus> verificationStatuses);

    @Query("select coalesce(sum(abs(s.varianceAmount)), 0) from CounterCashSession s "
            + "where s.status = :status and s.cinemaPublicId = :cinemaPublicId "
            + "and s.verificationStatus in :verificationStatuses")
    java.math.BigDecimal sumAbsoluteVarianceForCinema(
            @Param("status") CounterCashSessionStatus status,
            @Param("cinemaPublicId") String cinemaPublicId,
            @Param("verificationStatuses") List<CashVerificationStatus> verificationStatuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CounterCashSession s where s.publicId = :publicId")
    Optional<CounterCashSession> findByPublicIdForUpdate(@Param("publicId") String publicId);
}
