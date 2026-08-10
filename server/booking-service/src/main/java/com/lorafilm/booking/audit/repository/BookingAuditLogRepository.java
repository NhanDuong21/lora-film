package com.lorafilm.booking.audit.repository;

import com.lorafilm.booking.audit.entity.BookingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingAuditLogRepository extends JpaRepository<BookingAuditLog, Long> {

    Optional<BookingAuditLog> findByPublicId(String publicId);

    List<BookingAuditLog> findByBookingId(Long bookingId);
}
