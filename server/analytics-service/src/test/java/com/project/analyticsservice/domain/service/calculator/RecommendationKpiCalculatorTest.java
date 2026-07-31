package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.entity.Recommendation;
import com.project.analyticsservice.repository.BusinessInsightRepository;
import com.project.analyticsservice.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationKpiCalculatorTest {
    @Mock
    private BusinessInsightRepository insightRepository;
    @Mock
    private RecommendationRepository recommendationRepository;

    @Test
    void createsActionableRevenueRecommendationFromBusinessContext() {
        LocalDate date = LocalDate.of(2026, 7, 29);
        BusinessInsight insight = new BusinessInsight();
        insight.setId(10L);
        insight.setCategory("REVENUE_DROP");
        insight.setSeverity("HIGH");
        insight.setRootCause(
                "Ảnh hưởng lớn nhất đến từ LoraFilm Quận 10, nơi có doanh thu thấp hơn mức trung bình gần đây.");
        insight.setExpectedValue(new BigDecimal("1000000"));
        insight.setActualValue(new BigDecimal("700000"));
        insight.setConfidenceScore(new BigDecimal("0.85"));
        insight.setResolved(false);
        when(insightRepository.findAllByStatDate(date)).thenReturn(List.of(insight));
        when(recommendationRepository.findByInsightIdAndActionType(
                10L, "REVIEW_REVENUE_DRIVERS")).thenReturn(Optional.empty());

        new RecommendationKpiCalculator(insightRepository, recommendationRepository)
                .calculate(date);

        ArgumentCaptor<Recommendation> captor =
                ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendationRepository).save(captor.capture());
        Recommendation recommendation = captor.getValue();
        assertEquals("business-operations", recommendation.getTargetService());
        assertTrue(recommendation.getDescription().contains("LoraFilm Quận 10"));
        assertTrue(recommendation.getDescription().contains("Không giảm giá đồng loạt"));
        assertTrue(recommendation.getExpectedImpact().contains("300.000 đồng"));
        assertFalse(recommendation.getDescription().contains("root-cause"));
        assertFalse(recommendation.getDescription().contains("snapshot"));
    }
}
