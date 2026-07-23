package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class ShowtimeSchedulePreviewHistoryRepositoryImpl implements ShowtimeSchedulePreviewHistoryRepository {

    private final EntityManager entityManager;

    public ShowtimeSchedulePreviewHistoryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<ShowtimeSchedulePreviewHistoryRow> findHistory(
            AutoSchedulePreviewHistoryQuery filter,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<ShowtimeSchedulePreviewHistoryRow> contentCriteria =
                cb.createQuery(ShowtimeSchedulePreviewHistoryRow.class);
        Root<ShowtimeSchedulePreview> preview = contentCriteria.from(ShowtimeSchedulePreview.class);
        Join<ShowtimeSchedulePreview, Cinema> cinema = preview.join("cinema", JoinType.INNER);

        contentCriteria.select(cb.construct(
                ShowtimeSchedulePreviewHistoryRow.class,
                preview.get("publicId"),
                preview.get("version"),
                cinema.get("publicId"),
                cinema.get("name"),
                preview.get("timezoneSnapshot"),
                preview.get("scheduleFrom"),
                preview.get("scheduleTo"),
                preview.get("strategyVersion"),
                preview.get("applyMode"),
                preview.get("status"),
                preview.get("totalCandidateCount"),
                preview.get("validCandidateCount"),
                preview.get("rejectedCandidateCount"),
                preview.get("selectedCandidateCount"),
                preview.get("createdAt"),
                preview.get("expiresAt"),
                preview.get("appliedAt")
        ));
        contentCriteria.where(predicates(filter, cb, preview, cinema));
        contentCriteria.orderBy(orders(cb, preview, cinema, pageable.getSort()));

        TypedQuery<ShowtimeSchedulePreviewHistoryRow> contentQuery = entityManager.createQuery(contentCriteria);
        contentQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        contentQuery.setMaxResults(pageable.getPageSize());
        List<ShowtimeSchedulePreviewHistoryRow> content = contentQuery.getResultList();

        CriteriaQuery<Long> countCriteria = cb.createQuery(Long.class);
        Root<ShowtimeSchedulePreview> countPreview = countCriteria.from(ShowtimeSchedulePreview.class);
        Join<ShowtimeSchedulePreview, Cinema> countCinema = countPreview.join("cinema", JoinType.INNER);
        countCriteria.select(cb.count(countPreview));
        countCriteria.where(predicates(filter, cb, countPreview, countCinema));
        long total = entityManager.createQuery(countCriteria).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private Predicate[] predicates(
            AutoSchedulePreviewHistoryQuery filter,
            CriteriaBuilder cb,
            Root<ShowtimeSchedulePreview> preview,
            Join<ShowtimeSchedulePreview, Cinema> cinema
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (hasText(filter.getCinemaPublicId())) {
            predicates.add(cb.equal(cinema.get("publicId"), filter.getCinemaPublicId().trim()));
        }
        if (filter.getStatus() != null) {
            predicates.add(cb.equal(preview.get("status"), filter.getStatus()));
        }
        if (hasText(filter.getStrategyVersion())) {
            predicates.add(cb.equal(preview.get("strategyVersion"), filter.getStrategyVersion().trim()));
        }
        if (filter.getScheduleFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(preview.get("scheduleTo"), filter.getScheduleFrom()));
        }
        if (filter.getScheduleTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(preview.get("scheduleFrom"), filter.getScheduleTo()));
        }
        if (filter.getCreatedFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(preview.get("createdAt"), filter.getCreatedFrom()));
        }
        if (filter.getCreatedTo() != null) {
            predicates.add(cb.lessThan(preview.get("createdAt"), filter.getCreatedTo()));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private List<Order> orders(
            CriteriaBuilder cb,
            Root<ShowtimeSchedulePreview> preview,
            Join<ShowtimeSchedulePreview, Cinema> cinema,
            Sort sort
    ) {
        Sort.Order requested = sort.stream().findFirst()
                .orElseGet(() -> Sort.Order.desc("createdAt"));
        Expression<?> expression = sortExpression(cb, preview, cinema, requested.getProperty());
        List<Order> orders = new ArrayList<>();

        if ("appliedAt".equals(requested.getProperty())) {
            Path<Object> appliedAt = preview.get("appliedAt");
            Expression<Integer> nullRank = cb.<Integer>selectCase()
                    .when(cb.isNull(appliedAt), 1)
                    .otherwise(0);
            orders.add(cb.asc(nullRank));
        }

        orders.add(requested.isAscending() ? cb.asc(expression) : cb.desc(expression));
        orders.add(cb.desc(preview.get("id")));
        return orders;
    }

    private Expression<?> sortExpression(
            CriteriaBuilder cb,
            Root<ShowtimeSchedulePreview> preview,
            Join<ShowtimeSchedulePreview, Cinema> cinema,
            String property
    ) {
        return switch (property) {
            case "createdAt" -> preview.get("createdAt");
            case "scheduleFrom" -> preview.get("scheduleFrom");
            case "scheduleTo" -> preview.get("scheduleTo");
            case "status" -> preview.get("status");
            case "cinemaName" -> cb.lower(cinema.get("name"));
            case "totalCandidateCount" -> preview.get("totalCandidateCount");
            case "selectedCandidateCount" -> preview.get("selectedCandidateCount");
            case "appliedAt" -> preview.get("appliedAt");
            default -> throw new IllegalArgumentException("Unsupported history sort property: " + property);
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
