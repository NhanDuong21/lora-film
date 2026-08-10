package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.BookingSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingSnapshotRepository extends JpaRepository<BookingSnapshot, Long> {

    Optional<BookingSnapshot> findByPublicId(String publicId);

    Optional<BookingSnapshot> findByBookingId(Long bookingId);
}
