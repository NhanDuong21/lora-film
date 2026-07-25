package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.BookingPriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingPriceSnapshotRepository extends JpaRepository<BookingPriceSnapshot, Long> {
    Optional<BookingPriceSnapshot> findByBookingId(Long bookingId);
    boolean existsByBookingId(Long bookingId);
}
