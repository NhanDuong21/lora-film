package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.entity.BusinessAlert;
import com.project.analyticsservice.entity.Recommendation;
import com.project.analyticsservice.entity.RootCauseFactor;
import com.project.analyticsservice.repository.BusinessAlertRepository;
import com.project.analyticsservice.repository.RecommendationRepository;
import com.project.analyticsservice.repository.RootCauseFactorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsLifecycleDomainServiceTest {
    private final BusinessAlertRepository alertRepository = mock(BusinessAlertRepository.class);
    private final RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
    private final RootCauseFactorRepository rootCauseRepository = mock(RootCauseFactorRepository.class);
    private AnalyticsLifecycleDomainService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsLifecycleDomainService(
                alertRepository, recommendationRepository, rootCauseRepository);
    }

    @Test
    void acknowledgeAlert_ShouldRejectManagerWhenPrimaryCauseBelongsToAnotherCinema() {
        BusinessAlert alert = new BusinessAlert();
        alert.setId(12L);
        alert.setInsightId(3L);
        alert.setAcknowledged(false);
        when(alertRepository.findById(12L)).thenReturn(Optional.of(alert));
        when(rootCauseRepository.findAllByInsightIdInOrderByInsightIdAscRankOrderAsc(List.of(3L)))
                .thenReturn(List.of(rootCause(3L, 1, "cinema-2"), rootCause(3L, 2, "cinema-1")));

        assertThrows(AccessDeniedException.class,
                () -> service.acknowledgeAlert(12L, "manager@example.com", Set.of("cinema-1")));
        verify(alertRepository, never()).save(alert);
    }

    @Test
    void updateRecommendation_ShouldAllowManagerWhenPrimaryCauseBelongsToAssignedCinema() {
        Recommendation recommendation = new Recommendation();
        recommendation.setId(9L);
        recommendation.setInsightId(3L);
        recommendation.setStatus("PENDING");
        when(recommendationRepository.findById(9L)).thenReturn(Optional.of(recommendation));
        when(rootCauseRepository.findAllByInsightIdInOrderByInsightIdAscRankOrderAsc(List.of(3L)))
                .thenReturn(List.of(rootCause(3L, 1, "cinema-1")));

        var result = service.updateRecommendation(
                9L, "ACCEPTED", "manager@example.com", Set.of("cinema-1"));

        assertEquals("ACCEPTED", result.status());
        assertEquals("manager@example.com", recommendation.getAcceptedBy());
        verify(recommendationRepository).save(recommendation);
    }

    private RootCauseFactor rootCause(long insightId, int rank, String cinemaKey) {
        RootCauseFactor value = new RootCauseFactor();
        value.setInsightId(insightId);
        value.setRankOrder(rank);
        value.setCauseType("CINEMA_LOW_OCCUPANCY");
        value.setDimensionType("CINEMA");
        value.setDimensionKey(cinemaKey);
        value.setContributionScore(BigDecimal.ONE);
        return value;
    }
}
