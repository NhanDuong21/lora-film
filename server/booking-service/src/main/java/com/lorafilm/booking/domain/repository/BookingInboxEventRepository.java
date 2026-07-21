package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingInboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingInboxEventRepository extends JpaRepository<BookingInboxEvent, Long>, JpaSpecificationExecutor<BookingInboxEvent> {
    Optional<BookingInboxEvent> findByEventId(UUID eventId);
}
