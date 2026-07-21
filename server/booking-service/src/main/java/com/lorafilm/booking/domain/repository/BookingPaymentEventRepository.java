package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.BookingPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingPaymentEventRepository extends JpaRepository<BookingPaymentEvent, Long>, JpaSpecificationExecutor<BookingPaymentEvent> {
    Optional<BookingPaymentEvent> findByEventId(UUID eventId);
}
