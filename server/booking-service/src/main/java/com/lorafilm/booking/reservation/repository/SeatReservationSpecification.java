package com.lorafilm.booking.reservation.repository;

import com.lorafilm.booking.reservation.entity.SeatReservation;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import org.springframework.data.jpa.domain.Specification;

public class SeatReservationSpecification {

    public static Specification<SeatReservation> hasUserId(Long userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("userId"), userId);
    }

    public static Specification<SeatReservation> hasStatus(SeatReservationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<SeatReservation> hasShowtimeId(Long showtimeId) {
        return (root, query, cb) -> showtimeId == null ? null : cb.equal(root.get("showtimeId"), showtimeId);
    }
}
