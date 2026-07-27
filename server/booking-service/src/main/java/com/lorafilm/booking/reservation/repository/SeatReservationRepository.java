package com.lorafilm.booking.reservation.repository;

import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long>, JpaSpecificationExecutor<SeatReservation> {

    Optional<SeatReservation> findByPublicId(String publicId);

    Optional<SeatReservation> findByReservationCode(String reservationCode);

    List<SeatReservation> findAllByIdIn(List<Long> ids);

    List<SeatReservation> findAllByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from SeatReservation reservation where reservation.id in :ids")
    List<SeatReservation> findAllByIdInForUpdate(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from SeatReservation reservation where reservation.publicId in :publicIds")
    List<SeatReservation> findAllByPublicIdInForUpdate(@Param("publicIds") List<String> publicIds);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sr FROM SeatReservation sr
        WHERE sr.showtimeId = :showtimeId
          AND sr.seatId IN :seatIds
          AND sr.status IN ('HELD', 'BOOKED')
        ORDER BY sr.seatId, sr.id
        """)
    List<SeatReservation> findReservationsForBookingUpdate(
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") List<Long> seatIds);

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

    @Query("""
        SELECT sr FROM SeatReservation sr
        WHERE sr.status = 'HELD' AND sr.bookingId IS NULL AND sr.expiresAt <= :now
        """)
    List<SeatReservation> findExpiredUnlinkedReservations(
            @Param("now") Instant now, Pageable pageable);

    @Query("""
        SELECT COUNT(sr) FROM SeatReservation sr 
        WHERE sr.userId = :userId 
          AND sr.showtimeId = :showtimeId 
          AND sr.status = 'HELD' 
          AND sr.expiresAt > :now
    """)
    long countActiveHeldSeatsByUserAndShowtime(
        @Param("userId") Long userId,
        @Param("showtimeId") Long showtimeId,
        @Param("now") Instant now
    );

    @Query("""
        SELECT sr FROM SeatReservation sr 
        WHERE sr.showtimeId = :showtimeId 
          AND (sr.status = 'BOOKED' OR (sr.status = 'HELD' AND sr.expiresAt > :now))
    """)
    List<SeatReservation> findAllActiveReservationsByShowtimeId(
        @Param("showtimeId") Long showtimeId,
        @Param("now") Instant now
    );

    @Query("""
        SELECT sr FROM SeatReservation sr
        WHERE sr.showtimePublicId = :showtimePublicId
          AND (sr.status = 'BOOKED' OR (sr.status = 'HELD' AND sr.expiresAt > :now))
        ORDER BY sr.seatPublicId
        """)
    List<SeatReservation> findAllActiveReservationsByShowtimePublicId(
            @Param("showtimePublicId") String showtimePublicId, @Param("now") Instant now);

    @Query("""
        SELECT DISTINCT bt.seatId FROM BookingTicket bt 
        JOIN bt.booking b 
        WHERE b.showtimeId = :showtimeId 
          AND b.bookingStatus IN (com.lorafilm.booking.booking.enums.BookingStatus.PENDING_PAYMENT, com.lorafilm.booking.booking.enums.BookingStatus.CONFIRMED, com.lorafilm.booking.booking.enums.BookingStatus.COMPLETED)
    """)
    List<Long> findSoldSeatIdsFromBookingsByShowtimeId(
        @Param("showtimeId") Long showtimeId
    );

    Page<SeatReservation> findAllByUserId(Long userId, Pageable pageable);

    Page<SeatReservation> findAllByUserIdAndStatus(Long userId, SeatReservationStatus status, Pageable pageable);

    Page<SeatReservation> findAllByUserIdAndShowtimeId(Long userId, Long showtimeId, Pageable pageable);

    Page<SeatReservation> findAllByUserIdAndShowtimeIdAndStatus(Long userId, Long showtimeId, SeatReservationStatus status, Pageable pageable);
}
