package com.lorafilm.booking.domain.specification;

import com.lorafilm.booking.domain.entity.BookingPaymentEvent;
import org.springframework.data.jpa.domain.Specification;

public class PaymentEventSpecification {
    // Empty specification class foundation for BookingPaymentEvent entity
    public static Specification<BookingPaymentEvent> empty() {
        return (root, query, cb) -> cb.conjunction();
    }
}
