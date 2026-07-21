package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.BookingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingTicketRepository extends JpaRepository<BookingTicket, Long> {

    Optional<BookingTicket> findByPublicId(String publicId);

    Optional<BookingTicket> findByTicketCode(String ticketCode);

    List<BookingTicket> findByBookingId(Long bookingId);
}
