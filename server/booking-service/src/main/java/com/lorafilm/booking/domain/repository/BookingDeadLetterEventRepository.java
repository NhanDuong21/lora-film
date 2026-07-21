package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingDeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingDeadLetterEventRepository extends JpaRepository<BookingDeadLetterEvent, Long>, JpaSpecificationExecutor<BookingDeadLetterEvent> {
    Optional<BookingDeadLetterEvent> findByPublicId(UUID publicId);
}
