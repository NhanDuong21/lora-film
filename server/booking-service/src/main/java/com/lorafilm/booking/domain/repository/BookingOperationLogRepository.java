package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingOperationLogRepository extends JpaRepository<BookingOperationLog, Long>, JpaSpecificationExecutor<BookingOperationLog> {
    Optional<BookingOperationLog> findByPublicId(UUID publicId);
}
