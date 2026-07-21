package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingOutboxEventRepository extends JpaRepository<BookingOutboxEvent, Long>, JpaSpecificationExecutor<BookingOutboxEvent> {
    Optional<BookingOutboxEvent> findByEventId(UUID eventId);
}
