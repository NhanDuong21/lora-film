package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingTicketRepository extends JpaRepository<BookingTicket, Long>, JpaSpecificationExecutor<BookingTicket> {
    List<BookingTicket> findByBookingId(Long bookingId);
}
