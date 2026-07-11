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

    public static Specification<Movie> hasGenreId(Long genreId) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.lorafilm.movie.movie.domain.entity.MovieGenre> genreRoot = subquery.from(com.lorafilm.movie.movie.domain.entity.MovieGenre.class);
            subquery.select(genreRoot.get("movie").get("id"))
                    .where(cb.equal(genreRoot.get("genre").get("id"), genreId));
            return cb.in(root.get("id")).value(subquery);
        };
    }

    public static Specification<Movie> hasShowtimeInCity(String city) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.lorafilm.movie.showtime.domain.entity.Showtime> showtimeRoot = subquery.from(com.lorafilm.movie.showtime.domain.entity.Showtime.class);
            subquery.select(showtimeRoot.get("movie").get("id"))
                    .where(
                        cb.equal(cb.lower(showtimeRoot.get("cinema").get("city")), city.toLowerCase()),
                        cb.equal(showtimeRoot.get("status"), com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.OPEN_FOR_BOOKING)
                    );
            return cb.in(root.get("id")).value(subquery);
        };
    }

    public static Specification<Movie> hasShowtimeInCinema(Long cinemaId) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.lorafilm.movie.showtime.domain.entity.Showtime> showtimeRoot = subquery.from(com.lorafilm.movie.showtime.domain.entity.Showtime.class);
            subquery.select(showtimeRoot.get("movie").get("id"))
                    .where(
                        cb.equal(showtimeRoot.get("cinema").get("id"), cinemaId),
                        cb.equal(showtimeRoot.get("status"), com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.OPEN_FOR_BOOKING)
                    );
            return cb.in(root.get("id")).value(subquery);
        };
    }

    public static Specification<Movie> hasShowtimeOnDate(java.time.LocalDate date) {
        return (root, query, cb) -> {
            java.time.Instant startOfDay = date.atStartOfDay().toInstant(java.time.ZoneOffset.ofHours(7));
            java.time.Instant endOfDay = date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.ofHours(7));
            
            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<com.lorafilm.movie.showtime.domain.entity.Showtime> showtimeRoot = subquery.from(com.lorafilm.movie.showtime.domain.entity.Showtime.class);
            subquery.select(showtimeRoot.get("movie").get("id"))
                    .where(
                        cb.between(showtimeRoot.get("startTime"), startOfDay, endOfDay),
                        cb.equal(showtimeRoot.get("status"), com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.OPEN_FOR_BOOKING)
                    );
            return cb.in(root.get("id")).value(subquery);
        };
    }
}
