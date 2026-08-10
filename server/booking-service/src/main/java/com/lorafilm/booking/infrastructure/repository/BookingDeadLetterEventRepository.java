package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingDeadLetterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingDeadLetterEventRepository extends JpaRepository<BookingDeadLetterEvent, Long> {

    Optional<BookingDeadLetterEvent> findByPublicId(String publicId);

    Optional<BookingDeadLetterEvent> findByEventId(String eventId);
}
