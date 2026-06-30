package com.project.paymentservice.repository;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findByPaymentTransactionCode(String code);

    Optional<Payment> findByExternalTransactionId(String externalId);

    Page<Payment> findByBookingId(Long bookingId, Pageable pageable);

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDescIdDesc(Long bookingId);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    Optional<Payment> findFirstByBookingIdAndStatusInAndExpiresAtAfterOrderByCreatedAtDesc(Collection<PaymentStatus> statuses, LocalDateTime time);

    Page<Payment> findByStatusInAndExpiresAtBefore(Collection<PaymentStatus> statuses, LocalDateTime time, Pageable pageable);
}
