package com.project.paymentservice.repository;

import com.project.paymentservice.entity.BookingPaymentGuard;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingPaymentGuardRepository extends JpaRepository<BookingPaymentGuard, String> {
    Optional<BookingPaymentGuard> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from BookingPaymentGuard g where g.bookingPublicId = :bookingPublicId")
    Optional<BookingPaymentGuard> findByBookingPublicIdForUpdate(@Param("bookingPublicId") String bookingPublicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from BookingPaymentGuard g where g.bookingId = :bookingId")
    Optional<BookingPaymentGuard> findByBookingIdForUpdate(@Param("bookingId") Long bookingId);

    @Modifying
    @Query(value = """
            insert ignore into booking_payment_guards
                (booking_public_id, booking_id, next_attempt_number, version)
            values (:bookingPublicId, :bookingId, 1, 0)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("bookingPublicId") String bookingPublicId, @Param("bookingId") Long bookingId);
}
