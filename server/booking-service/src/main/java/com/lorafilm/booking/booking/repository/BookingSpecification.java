package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BookingSpecification {

    public static Specification<Booking> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("bookingStatus"), status);
    }

    public static Specification<Booking> filterBy(BookingFilterRequest filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getBookingCode() != null && !filter.getBookingCode().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("bookingCode")), "%" + filter.getBookingCode().trim().toLowerCase() + "%"));
            }

            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("bookingStatus"), filter.getStatus()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
