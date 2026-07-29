package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.dto.AnalyticsResponses;
import com.project.analyticsservice.entity.BusinessAlert;
import com.project.analyticsservice.entity.Recommendation;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.BusinessAlertRepository;
import com.project.analyticsservice.repository.RecommendationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class AnalyticsLifecycleDomainService {
    private static final Set<String> RECOMMENDATION_STATUSES =
            Set.of("PENDING", "ACCEPTED", "DISMISSED", "COMPLETED");

    private final BusinessAlertRepository alertRepository;
    private final RecommendationRepository recommendationRepository;

    public AnalyticsLifecycleDomainService(
            BusinessAlertRepository alertRepository,
            RecommendationRepository recommendationRepository) {
        this.alertRepository = alertRepository;
        this.recommendationRepository = recommendationRepository;
    }

    @Transactional
    public AnalyticsResponses.ActionResult acknowledgeAlert(long id, String actor) {
        BusinessAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> notFound("Alert not found", "ANALYTICS_ALERT_NOT_FOUND"));
        if (!Boolean.TRUE.equals(alert.getAcknowledged())) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedBy(normalizeActor(actor));
            alert.setAcknowledgedAt(Instant.now());
            alertRepository.save(alert);
        }
        return new AnalyticsResponses.ActionResult(
                alert.getId(), "ACKNOWLEDGED", alert.getAcknowledgedAt());
    }

    @Transactional
    public AnalyticsResponses.ActionResult updateRecommendation(
            long id, String requestedStatus, String actor) {
        Recommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> notFound(
                        "Recommendation not found", "ANALYTICS_RECOMMENDATION_NOT_FOUND"));
        String status = requestedStatus == null
                ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!RECOMMENDATION_STATUSES.contains(status)) {
            throw new BusinessException(
                    "status must be PENDING, ACCEPTED, DISMISSED or COMPLETED",
                    "ANALYTICS_INVALID_RECOMMENDATION_STATUS",
                    HttpStatus.BAD_REQUEST);
        }
        Instant now = Instant.now();
        recommendation.setStatus(status);
        if ("ACCEPTED".equals(status)) {
            recommendation.setAcceptedBy(normalizeActor(actor));
            recommendation.setAcceptedAt(now);
            recommendation.setCompletedAt(null);
        } else if ("COMPLETED".equals(status)) {
            if (recommendation.getAcceptedAt() == null) {
                recommendation.setAcceptedBy(normalizeActor(actor));
                recommendation.setAcceptedAt(now);
            }
            recommendation.setCompletedAt(now);
        }
        recommendationRepository.save(recommendation);
        return new AnalyticsResponses.ActionResult(id, status, now);
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "unknown" : actor;
    }

    private BusinessException notFound(String message, String code) {
        return new BusinessException(message, code, HttpStatus.NOT_FOUND);
    }
}
