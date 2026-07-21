package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingAuditLogRepository extends JpaRepository<BookingAuditLog, Long>, JpaSpecificationExecutor<BookingAuditLog> {
    Optional<BookingAuditLog> findByPublicId(UUID publicId);
}
