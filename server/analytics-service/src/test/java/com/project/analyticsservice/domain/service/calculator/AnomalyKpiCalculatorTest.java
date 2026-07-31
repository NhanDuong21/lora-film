package com.project.analyticsservice.domain.service.calculator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.AnomalyDetection;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.repository.AnomalyDetectionRepository;
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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyKpiCalculatorTest {
    @Mock
    private DailyBusinessKpiRepository dailyRepository;
    @Mock
    private AnomalyDetectionRepository anomalyRepository;

    @Test
    void detectsLargeRevenueDeviationAgainstRollingBaseline() {
        LocalDate date = LocalDate.of(2026, 7, 29);
        DailyBusinessKpi current = kpi(date, "50");
        List<DailyBusinessKpi> baseline = IntStream.range(0, 14)
                .mapToObj(index -> kpi(
                        date.minusDays(14 - index),
                        index % 2 == 0 ? "90" : "110"))
                .toList();
        when(dailyRepository.findByStatDate(date)).thenReturn(Optional.of(current));
        when(dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                date.minusDays(56), date.minusDays(1))).thenReturn(baseline);
        when(anomalyRepository.findAllByStatDate(date)).thenReturn(List.of());
        when(anomalyRepository.findByFingerprint(anyString())).thenReturn(Optional.empty());

        new AnomalyKpiCalculator(
                dailyRepository, anomalyRepository, new MetricMathService(),
                new ObjectMapper().findAndRegisterModules(),
                new BigDecimal("2.0"), new BigDecimal("0.20"))
                .calculate(date);

        ArgumentCaptor<AnomalyDetection> captor =
                ArgumentCaptor.forClass(AnomalyDetection.class);
        verify(anomalyRepository, atLeastOnce()).save(captor.capture());
        AnomalyDetection revenueAnomaly = captor.getAllValues().stream()
                .filter(value -> "NET_REVENUE".equals(value.getMetricName()))
                .findFirst().orElseThrow();
        assertEquals("ACTIVE", revenueAnomaly.getStatus());
        assertEquals(new BigDecimal("-0.500000"), revenueAnomaly.getDeviationRate());
        assertTrue(revenueAnomaly.getAnomalyScore().compareTo(new BigDecimal("3")) > 0);
        assertEquals("WEEKDAY_ROBUST_MAD_56D_V2", revenueAnomaly.getDetectionMethod());
    }

    private DailyBusinessKpi kpi(LocalDate date, String revenue) {
        DailyBusinessKpi value = new DailyBusinessKpi();
        value.setStatDate(date);
        value.setNetRevenue(new BigDecimal(revenue));
        value.setTicketCount(100L);
        value.setRefundRate(new BigDecimal("0.02"));
        value.setOccupancyRate(new BigDecimal("0.50"));
        return value;
    }
}
