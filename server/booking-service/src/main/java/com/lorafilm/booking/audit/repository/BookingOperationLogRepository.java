package com.lorafilm.booking.audit.repository;

import com.lorafilm.booking.audit.entity.BookingOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingOperationLogRepository extends JpaRepository<BookingOperationLog, Long> {

    Optional<BookingOperationLog> findByPublicId(String publicId);

    List<BookingOperationLog> findByBookingId(Long bookingId);
}
