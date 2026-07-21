package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import org.springframework.data.jpa.domain.Specification;

public class BookingSpecification {

    public static Specification<Booking> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("bookingStatus"), status);
    }
}
