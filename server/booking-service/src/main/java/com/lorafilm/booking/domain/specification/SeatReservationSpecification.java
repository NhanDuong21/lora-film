package com.lorafilm.booking.domain.specification;

import com.lorafilm.booking.domain.entity.SeatReservation;
import org.springframework.data.jpa.domain.Specification;

public class SeatReservationSpecification {
    // Empty specification class foundation for SeatReservation entity
    public static Specification<SeatReservation> empty() {
        return (root, query, cb) -> cb.conjunction();
    }
}
