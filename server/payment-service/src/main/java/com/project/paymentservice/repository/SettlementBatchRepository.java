package com.project.paymentservice.repository;

import com.project.paymentservice.entity.SettlementBatch;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.SettlementBatchStatus;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {
    boolean existsByProviderCodeAndBatchCode(ProviderCode providerCode, String batchCode);
    Optional<SettlementBatch> findByPublicId(String publicId);
    Page<SettlementBatch> findByStatus(SettlementBatchStatus status, Pageable pageable);
    Page<SettlementBatch> findByCinemaPublicId(String cinemaPublicId, Pageable pageable);
    Page<SettlementBatch> findByCinemaPublicIdAndStatus(
            String cinemaPublicId, SettlementBatchStatus status, Pageable pageable);
    long countByStatus(SettlementBatchStatus status);
    long countByStatusAndCinemaPublicId(SettlementBatchStatus status, String cinemaPublicId);
    long countByStatusAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            SettlementBatchStatus status, LocalDate periodEnd, LocalDate periodStart);
    long countByStatusAndCinemaPublicIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            SettlementBatchStatus status, String cinemaPublicId,
            LocalDate periodEnd, LocalDate periodStart);
}
