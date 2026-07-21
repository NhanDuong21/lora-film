package com.lorafilm.booking.reservation.repository;

import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long>, JpaSpecificationExecutor<SeatReservation> {

    Optional<SeatReservation> findByPublicId(String publicId);

    Optional<SeatReservation> findByReservationCode(String reservationCode);

    List<SeatReservation> findAllByIdIn(List<Long> ids);

    @Query("""
        SELECT sr FROM SeatReservation sr 
        WHERE sr.showtimeId = :showtimeId 
          AND sr.seatId IN :seatIds 
          AND (sr.status = 'BOOKED' OR (sr.status = 'HELD' AND sr.expiresAt > :now))
    """)
    List<SeatReservation> findActiveReservations(
        @Param("showtimeId") Long showtimeId,
        @Param("seatIds") List<Long> seatIds,
        @Param("now") Instant now
    );

    @Query("""
        SELECT bt.seatId FROM BookingTicket bt 
        JOIN bt.booking b 
        WHERE b.showtimeId = :showtimeId 
          AND bt.seatId IN :seatIds 
          AND b.bookingStatus IN (com.lorafilm.booking.booking.enums.BookingStatus.PENDING_PAYMENT, com.lorafilm.booking.booking.enums.BookingStatus.CONFIRMED, com.lorafilm.booking.booking.enums.BookingStatus.COMPLETED)
    """)
    List<Long> findSoldSeatIdsFromBookings(
        @Param("showtimeId") Long showtimeId,
        @Param("seatIds") List<Long> seatIds
    );

    @Query("""
        SELECT sr FROM SeatReservation sr 
        WHERE sr.status = 'HELD' AND sr.expiresAt <= :now
    """)
    List<SeatReservation> findExpiredReservations(
        @Param("now") Instant now,
        Pageable pageable
    );

    Page<SeatReservation> findAllByUserId(Long userId, Pageable pageable);

    Page<SeatReservation> findAllByUserIdAndStatus(Long userId, SeatReservationStatus status, Pageable pageable);

    Page<SeatReservation> findAllByUserIdAndShowtimeId(Long userId, Long showtimeId, Pageable pageable);

    Page<SeatReservation> findAllByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, SeatReservationStatus status, Pageable pageable);
}
