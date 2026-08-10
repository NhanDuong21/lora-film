package com.project.promotionservice.reservation.specification;

import com.project.promotionservice.reservation.entity.PromotionReservation;
import com.project.promotionservice.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    public static Specification<PromotionReservation> filter(
            ReservationStatus status,
            String userPublicId,
            String bookingPublicId,
            String orderPublicId,
            Instant from,
            Instant to) {
        return Specification.where(notDeleted())
                .and(equal("status", status))
                .and(equal("userPublicId", blankToNull(userPublicId)))
                .and(equal("bookingPublicId", blankToNull(bookingPublicId)))
                .and(equal("orderPublicId", blankToNull(orderPublicId)))
                .and(createdAtFrom(from))
                .and(createdAtTo(to));
    }

    private static Specification<PromotionReservation> notDeleted() {
        return (root, query, builder) -> builder.isNull(root.get("deletedAt"));
    }

    private static <T> Specification<PromotionReservation> equal(String field, T value) {
        return value == null
                ? null
                : (root, query, builder) -> builder.equal(root.get(field), value);
    }

    private static Specification<PromotionReservation> createdAtFrom(Instant from) {
        return from == null ? null : (root, query, builder) ->
                builder.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from);
    }

    private static Specification<PromotionReservation> createdAtTo(Instant to) {
        return to == null ? null : (root, query, builder) ->
                builder.lessThanOrEqualTo(root.<Instant>get("createdAt"), to);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
