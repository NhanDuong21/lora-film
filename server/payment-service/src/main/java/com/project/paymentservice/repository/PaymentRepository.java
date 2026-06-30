package com.project.paymentservice.repository;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByPaymentTransactionCode(String code);

    Optional<Payment> findByExternalTransactionId(String externalId);

    Page<Payment> findByBookingId(Long bookingId, Pageable pageable);

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDescIdDesc(Long bookingId);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.bookingId = :bookingId AND p.status IN :statuses AND p.expiresAt > :currentTime ORDER BY p.createdAt DESC, p.id DESC")
    List<Payment> findActiveAttempts(@Param("bookingId") Long bookingId, @Param("statuses") Collection<PaymentStatus> statuses, @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses AND p.expiresAt < :currentTime")
    Page<Payment> findExpiredActivePayments(@Param("statuses") Collection<PaymentStatus> statuses, @Param("currentTime") LocalDateTime currentTime, Pageable pageable);
}
