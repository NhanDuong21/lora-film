package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {

    List<BookingStatusHistory> findByBookingId(Long bookingId);

    List<BookingStatusHistory> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
