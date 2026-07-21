package com.lorafilm.booking.payment.repository;

import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingPaymentEventRepository extends JpaRepository<BookingPaymentEvent, Long> {

    Optional<BookingPaymentEvent> findByPublicId(String publicId);

    List<BookingPaymentEvent> findByBookingId(Long bookingId);
}
