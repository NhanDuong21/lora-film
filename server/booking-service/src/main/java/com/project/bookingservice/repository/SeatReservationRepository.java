package com.project.bookingservice.repository;

import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
    
    @Query("SELECT sr FROM SeatReservation sr " +
           "WHERE sr.showtimeId = :showtimeId AND sr.seatId IN :seatIds " +
           "AND (" +
           "  (sr.status = 'HELD' AND sr.expiresAt > CURRENT_TIMESTAMP) OR " +
           "  (sr.status = 'CONVERTED' AND EXISTS (" +
           "      SELECT b FROM Booking b WHERE b.id = sr.bookingId AND b.status IN ('PENDING_PAYMENT', 'CONFIRMED')" +
           "  ))" +
           ")")
    List<SeatReservation> findActiveReservations(
            @Param("showtimeId") Long showtimeId, 
            @Param("seatIds") List<Long> seatIds);

    List<SeatReservation> findByBookingId(Long bookingId);

    @Query("SELECT sr FROM SeatReservation sr WHERE sr.status = :status AND sr.expiresAt < :now ORDER BY sr.createdAt ASC")
    org.springframework.data.domain.Page<SeatReservation> findExpiredReservations(
            @Param("status") ReservationStatus status, 
            @Param("now") java.time.LocalDateTime now, 
            org.springframework.data.domain.Pageable pageable);
}
