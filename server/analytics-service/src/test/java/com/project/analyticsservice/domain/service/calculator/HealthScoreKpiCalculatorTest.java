package com.project.analyticsservice.domain.service.calculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.AnalyticsHealthScore;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.repository.AnalyticsHealthScoreRepository;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthScoreKpiCalculatorTest {
    @Mock
    private DailyBusinessKpiRepository dailyRepository;
    @Mock
    private AnalyticsHealthScoreRepository healthRepository;

    @Test
    void calculatesBackendOwnedWeightedHealthScore() {
        LocalDate date = LocalDate.of(2026, 7, 29);
        DailyBusinessKpi current = kpi(date, "1000", 100);
        current.setOccupancyRate(new BigDecimal("0.70"));
        current.setRefundRate(new BigDecimal("0.02"));
        current.setCancelRate(new BigDecimal("0.03"));
        current.setNewCustomerCount(20L);
        current.setReturningCustomerCount(30L);
        current.setDataCompleteness(new BigDecimal("0.90"));
        List<DailyBusinessKpi> baseline = java.util.stream.IntStream.rangeClosed(1, 7)
                .mapToObj(day -> kpi(date.minusDays(day), "1000", 100))
                .toList();

        when(dailyRepository.findByStatDate(date)).thenReturn(Optional.of(current));
        when(dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                date.minusDays(28), date.minusDays(1))).thenReturn(baseline);
        when(healthRepository.findByEntityTypeAndEntityKeyAndStatDate(
                "SYSTEM", "SYSTEM", date)).thenReturn(Optional.empty());

        new HealthScoreKpiCalculator(
                dailyRepository, healthRepository, new MetricMathService(),
                new ObjectMapper().findAndRegisterModules())
                .calculate(date);

        ArgumentCaptor<AnalyticsHealthScore> captor =
                ArgumentCaptor.forClass(AnalyticsHealthScore.class);
        verify(healthRepository).save(captor.capture());
        AnalyticsHealthScore value = captor.getValue();
        assertEquals("HEALTHY", value.getHealthStatus());
        assertEquals("HEALTH_SCORE_V2", value.getAlgorithmVersion());
        assertTrue(value.getOverallScore().compareTo(new BigDecimal("80")) >= 0);
        assertEquals(new BigDecimal("0.477500"), value.getConfidenceScore());
        assertEquals(new BigDecimal("90.000000"), value.getDataQualityScore());
    }

    private DailyBusinessKpi kpi(LocalDate date, String revenue, long tickets) {
        DailyBusinessKpi value = new DailyBusinessKpi();
        value.setStatDate(date);
        value.setNetRevenue(new BigDecimal(revenue));
        value.setTicketCount(tickets);
        value.setOccupancyRate(BigDecimal.ZERO);
        value.setRefundRate(BigDecimal.ZERO);
        value.setCancelRate(BigDecimal.ZERO);
        value.setNewCustomerCount(0L);
        value.setReturningCustomerCount(0L);
        value.setDataCompleteness(BigDecimal.ONE);
        return value;
    }
}
