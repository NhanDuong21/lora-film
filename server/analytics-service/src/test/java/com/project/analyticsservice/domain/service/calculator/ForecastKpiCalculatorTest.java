package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.entity.ForecastResult;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import com.project.analyticsservice.repository.ForecastModelMetricRepository;
import com.project.analyticsservice.repository.ForecastResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastKpiCalculatorTest {
    @Mock
    private DailyBusinessKpiRepository dailyRepository;
    @Mock
    private ForecastResultRepository forecastRepository;
    @Mock
    private ForecastModelMetricRepository modelMetricRepository;

    @Test
    void createsBoundedHybridForecastOnlyAfterEnoughHistory() {
        LocalDate date = LocalDate.of(2026, 7, 29);
        List<DailyBusinessKpi> history = IntStream.rangeClosed(0, 41)
                .mapToObj(index -> kpi(
                        date.minusDays(41L - index),
                        900_000L + index * 10_000L))
                .toList();
        when(dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                date.minusDays(83), date)).thenReturn(history);
        when(forecastRepository
                .findByEntityTypeAndEntityKeyAndForecastDateAndForecastType(
                        anyString(), anyString(), any(LocalDate.class), anyString()))
                .thenReturn(Optional.empty());

        new ForecastKpiCalculator(
                dailyRepository, forecastRepository, modelMetricRepository,
                new MetricMathService(), 7)
                .calculate(date);

        ArgumentCaptor<ForecastResult> captor =
                ArgumentCaptor.forClass(ForecastResult.class);
        verify(forecastRepository, atLeastOnce()).save(captor.capture());
        ForecastResult revenue = captor.getAllValues().stream()
                .filter(value -> "REVENUE".equals(value.getForecastType()))
                .findFirst().orElseThrow();
        assertEquals("HYBRID_WEEKDAY_RECENCY_DAMPED_TREND", revenue.getAlgorithm());
        assertEquals("3.0", revenue.getModelVersion());
        assertTrue(revenue.getPredictedValue().signum() > 0);
        assertTrue(revenue.getPredictionLowerBound()
                .compareTo(revenue.getPredictedValue()) <= 0);
        assertTrue(revenue.getPredictionUpperBound()
                .compareTo(revenue.getPredictedValue()) >= 0);
        assertTrue(revenue.getConfidenceScore()
                .compareTo(new BigDecimal("0.95")) <= 0);
    }

    @Test
    void doesNotForecastFromTooLittleHistory() {
        LocalDate date = LocalDate.of(2026, 7, 29);
        List<DailyBusinessKpi> history = IntStream.rangeClosed(0, 6)
                .mapToObj(index -> kpi(date.minusDays(6L - index), 1_000_000L))
                .toList();
        when(dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                date.minusDays(83), date)).thenReturn(history);

        new ForecastKpiCalculator(
                dailyRepository, forecastRepository, modelMetricRepository,
                new MetricMathService(), 7)
                .calculate(date);

        verify(forecastRepository, never()).save(any());
    }

    private DailyBusinessKpi kpi(LocalDate date, long revenue) {
        DailyBusinessKpi value = new DailyBusinessKpi();
        value.setStatDate(date);
        value.setNetRevenue(BigDecimal.valueOf(revenue));
        value.setTicketCount(Math.max(1, revenue / 100_000));
        value.setOccupancyRate(new BigDecimal("0.55"));
        value.setDataCompleteness(new BigDecimal("0.95"));
        return value;
    }
}
