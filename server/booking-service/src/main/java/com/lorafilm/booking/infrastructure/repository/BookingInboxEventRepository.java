package com.lorafilm.booking.infrastructure.repository;

import com.lorafilm.booking.infrastructure.entity.BookingInboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingInboxEventRepository extends JpaRepository<BookingInboxEvent, Long> {

    Optional<BookingInboxEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
