package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeSource;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;

public class ShowtimeSpecification {

    public static Specification<Showtime> isNotDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Showtime> hasStatus(ShowtimeStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Showtime> hasMovieSlug(String movieSlug) {
        return (root, query, cb) -> cb.equal(root.get("movie").get("slug"), movieSlug);
    }

    public static Specification<Showtime> hasCinemaSlug(String cinemaSlug) {
        return (root, query, cb) -> cb.equal(root.get("cinema").get("slug"), cinemaSlug);
    }

    public static Specification<Showtime> hasCity(String city) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("cinema").get("city")), city.toLowerCase());
    }

    public static Specification<Showtime> hasDate(LocalDate date) {
        return (root, query, cb) -> cb.equal(root.get("serviceDate"), date);
    }

    public static Specification<Showtime> hasMoviePublicId(String moviePublicId) {
        return (root, query, cb) -> cb.equal(root.get("movie").get("publicId"), moviePublicId);
    }

    public static Specification<Showtime> hasCinemaPublicId(String cinemaPublicId) {
        return (root, query, cb) -> cb.equal(root.get("cinema").get("publicId"), cinemaPublicId);
    }

    public static Specification<Showtime> hasStartTimeBetween(Instant start, Instant end) {
        return (root, query, cb) -> cb.between(root.get("startTime"), start, end);
    }

    public static Specification<Showtime> hasFormat(com.lorafilm.movie.movie.domain.enums.MovieFormat format) {
        return (root, query, cb) -> cb.equal(root.get("movieVersion").get("format"), format);
    }

    public static Specification<Showtime> hasAudioLanguage(String audioLanguage) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("movieVersion").get("audioLanguage")), audioLanguage.toLowerCase());
    }

    public static Specification<Showtime> hasSubtitleLanguage(String subtitleLanguage) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("movieVersion").get("subtitleLanguage")), subtitleLanguage.toLowerCase());
    }

    public static Specification<Showtime> hasBatchId(String batchId) {
        return (root, query, cb) -> cb.equal(root.get("batchId"), batchId);
    }

    public static Specification<Showtime> hasSource(ShowtimeSource source) {
        return (root, query, cb) -> cb.equal(root.get("source"), source);
    }
}
