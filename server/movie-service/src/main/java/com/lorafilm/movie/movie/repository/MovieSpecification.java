package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;

public class MovieSpecification {

    public static Specification<Movie> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Movie> isPubliclyVisible() {
        return (root, query, cb) -> root.get("status").in(MovieStatus.NOW_SHOWING, MovieStatus.UPCOMING);
    }

    public static Specification<Movie> hasStatus(MovieStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Movie> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), likePattern),
                cb.like(cb.lower(root.get("originalTitle")), likePattern)
            );
        };
    }
}
