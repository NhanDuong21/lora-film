package com.lorafilm.booking.payment.repository;

import com.lorafilm.booking.payment.entity.BookingPaymentEvent;
import org.springframework.data.jpa.domain.Specification;

public class PaymentEventSpecification {

    public static Specification<BookingPaymentEvent> hasBookingId(Long bookingId) {
        return (root, query, cb) -> bookingId == null ? null : cb.equal(root.get("booking").get("id"), bookingId);
    }
}
