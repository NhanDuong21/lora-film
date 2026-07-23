package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public final class MovieHealthSpecifications {

    private MovieHealthSpecifications() {
    }

    public static Specification<Movie> hasGenre() {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<MovieGenre> genre = subquery.from(MovieGenre.class);
            subquery.select(cb.literal(1))
                    .where(cb.equal(genre.get("movie").get("id"), root.get("id")));
            return cb.exists(subquery);
        };
    }

    public static Specification<Movie> hasActiveVersion() {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<MovieVersion> version = subquery.from(MovieVersion.class);
            subquery.select(cb.literal(1)).where(
                    cb.equal(version.get("movie").get("id"), root.get("id")),
                    cb.equal(version.get("status"), ActiveStatus.ACTIVE),
                    cb.isNull(version.get("deletedAt")));
            return cb.exists(subquery);
        };
    }

    public static Specification<Movie> hasActivePrimaryPoster() {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<MovieMedia> media = subquery.from(MovieMedia.class);
            subquery.select(cb.literal(1)).where(
                    cb.equal(media.get("movie").get("id"), root.get("id")),
                    cb.equal(media.get("mediaType"), MovieMediaType.POSTER),
                    cb.equal(media.get("status"), ActiveStatus.ACTIVE),
                    cb.isTrue(media.get("isPrimary")),
                    cb.isNull(media.get("deletedAt")));
            return cb.exists(subquery);
        };
    }

    public static Specification<Movie> hasAnyBlocker() {
        return (root, query, cb) -> cb.or(
                cb.not(toPredicate(hasGenre(), root, query, cb)),
                cb.not(toPredicate(hasActiveVersion(), root, query, cb)),
                cb.not(toPredicate(hasActivePrimaryPoster(), root, query, cb)));
    }

    public static Specification<Movie> hasAnyWarning() {
        return (root, query, cb) -> {
            Predicate invalidDuration = cb.or(
                    cb.isNull(root.get("durationMinutes")),
                    cb.lessThanOrEqualTo(root.get("durationMinutes"), 0));
            Predicate suspiciousDuration = cb.and(
                    cb.greaterThan(root.get("durationMinutes"), 0),
                    cb.lessThan(root.get("durationMinutes"), 30));
            return cb.or(
                    cb.isNull(root.get("title")),
                    cb.equal(cb.trim(root.get("title")), ""),
                    cb.isNull(root.get("releaseDate")),
                    cb.isNull(root.get("ageRating")),
                    invalidDuration,
                    suspiciousDuration);
        };
    }

    public static Specification<Movie> healthStatusEquals(MovieHealthStatus status) {
        return (root, query, cb) -> {
            Predicate blockers = toPredicate(hasAnyBlocker(), root, query, cb);
            Predicate warnings = toPredicate(hasAnyWarning(), root, query, cb);
            return switch (status) {
                case BLOCKED -> blockers;
                case WARNING -> cb.and(cb.not(blockers), warnings);
                case READY -> cb.and(cb.not(blockers), cb.not(warnings));
            };
        };
    }

    private static Predicate toPredicate(
            Specification<Movie> specification,
            Root<Movie> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate == null) {
            throw new IllegalStateException("Movie health specification produced no predicate");
        }
        return predicate;
    }
}
