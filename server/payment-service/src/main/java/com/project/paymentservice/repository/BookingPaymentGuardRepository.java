package com.project.paymentservice.repository;

import com.project.paymentservice.entity.BookingPaymentGuard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingPaymentGuardRepository extends JpaRepository<BookingPaymentGuard, Long> {

    Optional<BookingPaymentGuard> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM BookingPaymentGuard g WHERE g.bookingId = :bookingId")
    Optional<BookingPaymentGuard> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    @Modifying
    @Query(value = "INSERT IGNORE INTO booking_payment_guards (booking_id) VALUES (:bookingId)", nativeQuery = true)
    int insertIfAbsent(@Param("bookingId") Long bookingId);
}
