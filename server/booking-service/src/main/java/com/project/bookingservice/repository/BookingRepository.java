package com.project.bookingservice.repository;

import com.project.bookingservice.entity.Booking;
import com.project.bookingservice.enumtype.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByBookingCode(String bookingCode);

    @Query("SELECT b FROM Booking b WHERE b.userId = :userId " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (cast(:from as timestamp) IS NULL OR b.createdAt >= :from) " +
           "AND (cast(:to as timestamp) IS NULL OR b.createdAt <= :to)")
    Page<Booking> findMyBookings(
            @Param("userId") Long userId,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :now ORDER BY b.createdAt ASC")
    Page<Booking> findExpiredBookings(
            @Param("status") BookingStatus status, 
            @Param("now") LocalDateTime now, 
            Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    java.util.Optional<Booking> findByIdWithPessimisticLock(@Param("id") Long id);

    @Query("SELECT b FROM Booking b WHERE " +
           "(:userId IS NULL OR b.userId = :userId) " +
           "AND (:showtimeId IS NULL OR b.showtimeId = :showtimeId) " +
           "AND (:bookingCode IS NULL OR b.bookingCode = :bookingCode) " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (cast(:from as timestamp) IS NULL OR b.createdAt >= :from) " +
           "AND (cast(:to as timestamp) IS NULL OR b.createdAt <= :to)")
    Page<Booking> searchBookings(
            @Param("userId") Long userId,
            @Param("showtimeId") Long showtimeId,
            @Param("bookingCode") String bookingCode,
            @Param("status") BookingStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
