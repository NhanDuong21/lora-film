package com.project.paymentservice.repository;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByPublicId(String publicId);
    Optional<Payment> findByPublicIdAndAccountId(String publicId, Long accountId);
    Optional<Payment> findByPaymentTransactionCode(String paymentTransactionCode);
    Optional<Payment> findByProviderCodeAndProviderOrderId(ProviderCode providerCode, String providerOrderId);
    Optional<Payment> findByProviderCodeAndExternalTransactionId(ProviderCode providerCode, String externalTransactionId);
    Page<Payment> findByBookingPublicId(String bookingPublicId, Pageable pageable);
    Page<Payment> findByBookingPublicIdAndAccountId(String bookingPublicId, Long accountId, Pageable pageable);
    Page<Payment> findByBookingId(Long bookingId, Pageable pageable);
    Page<Payment> findByBookingIdAndAccountId(Long bookingId, Long accountId, Pageable pageable);
    Page<Payment> findByAccountId(Long accountId, Pageable pageable);
    Optional<Payment> findByBookingPublicIdAndAttemptNumber(String bookingPublicId, Integer attemptNumber);
    Page<Payment> findByReconciliationStatus(ReconciliationStatus status, Pageable pageable);
    Page<Payment> findByStatusAndBookingExpiresAtBefore(PaymentStatus status, Instant deadline, Pageable pageable);
    Page<Payment> findByStatusAndSettlementHoldUntilBefore(PaymentStatus status, Instant now, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.publicId = :publicId")
    Optional<Payment> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            where p.bookingPublicId in :bookingPublicIds
            order by p.id
            """)
    List<Payment> findByBookingPublicIdInForEmergencyUpdate(
            @Param("bookingPublicIds") List<String> bookingPublicIds);
}
