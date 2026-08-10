package com.lorafilm.movie.cinema.repository;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import org.springframework.data.jpa.domain.Specification;

public class CinemaSpecification {
    public static Specification<Cinema> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Cinema> hasStatus(CinemaStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Cinema> hasStatusIn(java.util.Collection<CinemaStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Cinema> hasCity(String city) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    public static Specification<Cinema> hasDistrict(String district) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("district")), district.toLowerCase());
    }

    public static Specification<Cinema> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), likePattern);
        };
    }
}
