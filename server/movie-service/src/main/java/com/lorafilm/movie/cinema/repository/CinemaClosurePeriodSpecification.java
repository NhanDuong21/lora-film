package com.lorafilm.movie.cinema.repository;

import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.common.enums.ActionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class CinemaClosurePeriodSpecification {

    private CinemaClosurePeriodSpecification() {
        // Prevent instantiation
    }

    public static Specification<CinemaClosurePeriod> hasCinemaId(Long cinemaId) {
        return (root, query, cb) -> cb.equal(root.get("cinema").get("id"), cinemaId);
    }

    public static Specification<CinemaClosurePeriod> hasStatus(ActionStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<CinemaClosurePeriod> isUpcoming() {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("endTime"), Instant.now());
    }
}
