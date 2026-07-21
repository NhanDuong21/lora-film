package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.enums.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingOutboxEventRepository extends JpaRepository<BookingOutboxEvent, Long> {

    Optional<BookingOutboxEvent> findByEventId(String eventId);

    List<BookingOutboxEvent> findByStatusAndNextRetryAtBefore(OutboxStatus status, Instant now, Pageable pageable);
}
