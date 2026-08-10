package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.BookingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingTicketRepository extends JpaRepository<BookingTicket, Long> {

    Optional<BookingTicket> findByPublicId(String publicId);

    Optional<BookingTicket> findByTicketCode(String ticketCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ticket FROM BookingTicket ticket
            JOIN FETCH ticket.booking booking
            WHERE ticket.ticketCode = :code
               OR ticket.qrCode = :code
               OR ticket.barcode = :code
            """)
    Optional<BookingTicket> findByAnyCodeForUpdate(@Param("code") String code);

    @Query("""
            SELECT ticket FROM BookingTicket ticket
            JOIN FETCH ticket.booking booking
            WHERE booking.cinemaPublicId = :cinemaPublicId
              AND ticket.showtimeStart >= :from
              AND ticket.showtimeStart < :to
            ORDER BY ticket.showtimeStart ASC, ticket.auditoriumName ASC, ticket.seatLabel ASC
            """)
    List<BookingTicket> findOperationalTickets(
            @Param("cinemaPublicId") String cinemaPublicId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    List<BookingTicket> findByBookingId(Long bookingId);

    List<BookingTicket> findByBookingIdOrderBySeatLabelAsc(Long bookingId);
}
