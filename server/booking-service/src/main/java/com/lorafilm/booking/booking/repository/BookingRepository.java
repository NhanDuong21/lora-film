package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByPublicId(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.isDeleted = false")
    Optional<Booking> findByIdForPaymentUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.publicId = :publicId")
    Optional<Booking> findByPublicIdWithLock(@Param("publicId") String publicId);

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndBookingStatus(Long userId, BookingStatus bookingStatus, Pageable pageable);

    long countByCreatedAtAfter(java.time.Instant start);

    long countByPaymentStatus(com.lorafilm.booking.booking.enums.PaymentStatus status);

    long countByBookingStatus(com.lorafilm.booking.booking.enums.BookingStatus status);

    @Query("SELECT b FROM Booking b " +
           "WHERE b.bookingStatus = :status " +
           "AND b.expiresAt <= :now")
    List<Booking> findExpiredBookings(@Param("status") BookingStatus status,
                                      @Param("now") java.time.Instant now,
                                      Pageable pageable);
}
