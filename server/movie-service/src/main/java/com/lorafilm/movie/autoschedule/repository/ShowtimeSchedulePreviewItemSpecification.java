package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.dto.request.ShowtimeSchedulePreviewItemQuery;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class ShowtimeSchedulePreviewItemSpecification {

    public static Specification<ShowtimeSchedulePreviewItem> filterBy(
            Long previewId,
            ShowtimeSchedulePreviewItemQuery query,
            String timezoneSnapshot
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by preview ID
            predicates.add(cb.equal(root.get("preview").get("id"), previewId));

            if (query.getSelected() != null) {
                predicates.add(cb.equal(root.get("selected"), query.getSelected()));
            }

            if (query.getValidationStatus() != null) {
                predicates.add(cb.equal(root.get("validationStatus"), query.getValidationStatus()));
            }

            if (query.getApplyStatus() != null) {
                predicates.add(cb.equal(root.get("applyStatus"), query.getApplyStatus()));
            }

            if (query.getAuditoriumPublicId() != null && !query.getAuditoriumPublicId().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("auditorium").get("publicId"), query.getAuditoriumPublicId().trim()));
            }

            if (query.getMovieVersionPublicId() != null && !query.getMovieVersionPublicId().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("movieVersion").get("publicId"), query.getMovieVersionPublicId().trim()));
            }

            if (query.getDate() != null) {
                LocalDate date = query.getDate();
                ZoneId zoneId = ZoneId.of(timezoneSnapshot);
                Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
                Instant startOfNextDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();
                
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), startOfDay));
                predicates.add(cb.lessThan(root.get("startTime"), startOfNextDay));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
