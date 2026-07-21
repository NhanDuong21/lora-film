package com.lorafilm.booking.domain.specification;

import com.lorafilm.booking.domain.entity.Booking;
import org.springframework.data.jpa.domain.Specification;

public class BookingSpecification {
    // Empty specification class foundation for Booking entity
    public static Specification<Booking> empty() {
        return (root, query, cb) -> cb.conjunction();
    }
}
