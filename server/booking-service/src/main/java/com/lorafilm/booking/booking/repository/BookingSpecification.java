package com.lorafilm.booking.booking.repository;

import com.lorafilm.booking.booking.dto.BookingFilterRequest;
import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.enums.BookingAttentionFilter;
import com.lorafilm.booking.booking.enums.PaymentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BookingSpecification {

    private BookingSpecification() {
    }

    public static Specification<Booking> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Booking> hasStatus(BookingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("bookingStatus"), status);
    }

    public static Specification<Booking> filterBy(BookingFilterRequest filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return cb.isFalse(root.get("isDeleted"));
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (filter.getBookingCode() != null && !filter.getBookingCode().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("bookingCode")), "%" + filter.getBookingCode().trim().toLowerCase() + "%"));
            }

            if (filter.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.getUserId()));
            }

            if (filter.getUserIds() != null && !filter.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(filter.getUserIds()));
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

            if (filter.getAttention() != null) {
                Predicate attentionPredicate = attentionPredicate(
                        filter.getAttention(), root, cb, Instant.now());
                predicates.add(attentionPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Booking> attention(BookingAttentionFilter attention, Instant now) {
        return (root, query, cb) -> attention == null
                ? cb.conjunction()
                : attentionPredicate(attention, root, cb, now);
    }

    private static Predicate attentionPredicate(
            BookingAttentionFilter attention,
            jakarta.persistence.criteria.Root<Booking> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Instant now) {
        Instant expiringSoonAt = now.plus(5, ChronoUnit.MINUTES);
        Predicate pending = cb.equal(root.get("bookingStatus"), BookingStatus.PENDING_PAYMENT);
        Predicate overdue = cb.and(
                pending,
                cb.lessThanOrEqualTo(root.get("expiresAt"), now));
        Predicate expiringSoon = cb.and(
                pending,
                cb.greaterThan(root.get("expiresAt"), now),
                cb.lessThanOrEqualTo(root.get("expiresAt"), expiringSoonAt));
        Predicate paymentFailed = cb.and(
                pending,
                cb.equal(root.get("paymentStatus"), PaymentStatus.FAILED));

        return switch (attention) {
            case OVERDUE -> overdue;
            case EXPIRING_SOON -> expiringSoon;
            case PAYMENT_FAILED -> paymentFailed;
            case NEEDS_ATTENTION -> cb.or(overdue, expiringSoon, paymentFailed);
        };
    }

    public static Specification<Booking> createdFrom(Instant fromDate) {
        return (root, query, cb) -> fromDate == null
                ? null
                : cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate);
    }

    public static Specification<Booking> createdTo(Instant toDate) {
        return (root, query, cb) -> toDate == null
                ? null
                : cb.lessThanOrEqualTo(root.get("createdAt"), toDate);
    }

    public static Specification<Booking> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }
}
