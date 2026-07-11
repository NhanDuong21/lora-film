package com.project.bookingservice.repository;

import com.project.bookingservice.entity.BookingPaymentResultEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingPaymentResultEventRepository extends JpaRepository<BookingPaymentResultEvent, Long> {
    
    Optional<BookingPaymentResultEvent> findByEventId(String eventId);
}
