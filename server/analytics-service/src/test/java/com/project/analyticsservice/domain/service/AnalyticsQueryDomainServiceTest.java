package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.dto.AnalyticsResponses;
import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.entity.CinemaPerformanceDaily;
import com.project.analyticsservice.entity.RootCauseFactor;
import com.project.analyticsservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsQueryDomainServiceTest {
    private final DailyBusinessKpiRepository dailyRepository = mock(DailyBusinessKpiRepository.class);
    private final CinemaPerformanceDailyRepository cinemaRepository =
            mock(CinemaPerformanceDailyRepository.class);
    private final MoviePerformanceDailyRepository movieRepository =
            mock(MoviePerformanceDailyRepository.class);
    private final PromotionPerformanceDailyRepository promotionRepository =
            mock(PromotionPerformanceDailyRepository.class);
    private final CustomerSegmentDailyRepository segmentRepository =
            mock(CustomerSegmentDailyRepository.class);
    private final ForecastResultRepository forecastRepository = mock(ForecastResultRepository.class);
    private final BusinessInsightRepository insightRepository = mock(BusinessInsightRepository.class);
    private final RecommendationRepository recommendationRepository =
            mock(RecommendationRepository.class);
    private final BusinessAlertRepository alertRepository = mock(BusinessAlertRepository.class);
    private final FactBookingMetricRepository bookingFactRepository =
            mock(FactBookingMetricRepository.class);
    private final FactBookingCancellationRepository cancellationFactRepository =
            mock(FactBookingCancellationRepository.class);
    private final FactPaymentRefundRepository refundFactRepository =
            mock(FactPaymentRefundRepository.class);
    private final ProcessedAnalyticsEventRepository processedEventRepository =
            mock(ProcessedAnalyticsEventRepository.class);
    private final KpiCalculationRunRepository calculationRunRepository =
            mock(KpiCalculationRunRepository.class);
    private final AnalyticsHealthScoreRepository healthRepository =
            mock(AnalyticsHealthScoreRepository.class);
    private final AnomalyDetectionRepository anomalyRepository =
            mock(AnomalyDetectionRepository.class);
    private final RootCauseFactorRepository rootCauseRepository =
            mock(RootCauseFactorRepository.class);
    private final ForecastModelMetricRepository forecastMetricRepository =
            mock(ForecastModelMetricRepository.class);
    private final AnalyticsDataQualityDailyRepository qualityDailyRepository =
            mock(AnalyticsDataQualityDailyRepository.class);
    private final MetricMathService math = new MetricMathService();
    private final FactAnalysisService factAnalysisService = new FactAnalysisService(
            bookingFactRepository,
            cancellationFactRepository,
            refundFactRepository,
            math);

    private AnalyticsQueryDomainService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsQueryDomainService(
                dailyRepository,
                cinemaRepository,
                movieRepository,
                promotionRepository,
                segmentRepository,
                forecastRepository,
                insightRepository,
                recommendationRepository,
                alertRepository,
                bookingFactRepository,
                cancellationFactRepository,
                refundFactRepository,
                processedEventRepository,
                calculationRunRepository,
                healthRepository,
                anomalyRepository,
                rootCauseRepository,
                forecastMetricRepository,
                qualityDailyRepository,
                math,
                factAnalysisService,
                "Asia/Ho_Chi_Minh");

        when(insightRepository.findAllByResolvedFalseAndStatDateBetweenOrderByCreatedAtDesc(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of());
        when(calculationRunRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());
        when(dailyRepository.findFirstByOrderByStatDateDesc()).thenReturn(Optional.empty());
        when(qualityDailyRepository.findFirstByOrderByStatDateDescCalculatedAtDesc())
                .thenReturn(Optional.empty());
        when(bookingFactRepository.findAllByCinemaPublicIdAndBusinessDateBetween(
                "cinema-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of());
        when(refundFactRepository.findAllByRefundDateBetween(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of());
    }

    @Test
    void dashboard_ShouldReturnOnlySelectedCinemaKpis() {
        CinemaPerformanceDaily first = cinemaKpi(
                LocalDate.of(2026, 7, 1), "100000", 4, 8, "0.25", "0.50");
        CinemaPerformanceDaily second = cinemaKpi(
                LocalDate.of(2026, 7, 2), "150000", 6, 12, "0.00", "0.75");
        when(cinemaRepository.findAllByCinemaKeyAndStatDateBetweenOrderByStatDateAsc(
                "cinema-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of(first, second));

        AnalyticsResponses.Dashboard dashboard =
                service.dashboard("2026-07-01", "2026-07-02", "cinema-1");

        assertEquals("CINEMA", dashboard.scope().type());
        assertEquals("cinema-1", dashboard.scope().cinemaKey());
        assertEquals(new BigDecimal("250000.00"), dashboard.summary().netRevenue());
        assertEquals(10, dashboard.summary().bookingCount());
        assertEquals(2, dashboard.daily().size());
        assertEquals(1, dashboard.topCinemas().size());
        assertTrue(dashboard.forecasts().isEmpty());
    }

    @Test
    void dashboard_ShouldReturnEmptyCinemaViewWhenCinemaHasNoAnalyticsYet() {
        when(cinemaRepository.findAllByCinemaKeyAndStatDateBetweenOrderByStatDateAsc(
                "cinema-2", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of());
        when(cinemaRepository.findFirstByCinemaKeyOrderByStatDateDesc("cinema-2"))
                .thenReturn(Optional.empty());
        when(bookingFactRepository.findAllByCinemaPublicIdAndBusinessDateBetween(
                "cinema-2", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of());

        AnalyticsResponses.Dashboard dashboard =
                service.dashboard("2026-07-01", "2026-07-02", "cinema-2");

        assertEquals("CINEMA", dashboard.scope().type());
        assertEquals("cinema-2", dashboard.scope().cinemaKey());
        assertEquals(BigDecimal.ZERO.setScale(2), dashboard.summary().netRevenue());
        assertEquals(0, dashboard.summary().bookingCount());
        assertTrue(dashboard.daily().isEmpty());
    }

    @Test
    void dashboard_ShouldOnlyReturnInsightsWhosePrimaryCauseBelongsToSelectedCinema() {
        BusinessInsight secondaryCauseOnly = insight(1L, "Ảnh hưởng phụ tại rạp");
        BusinessInsight primaryCause = insight(2L, "Nguyên nhân chính tại rạp");
        when(insightRepository.findAllByResolvedFalseAndStatDateBetweenOrderByCreatedAtDesc(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of(secondaryCauseOnly, primaryCause));
        when(rootCauseRepository.findAllByInsightIdInOrderByInsightIdAscRankOrderAsc(
                List.of(1L, 2L)))
                .thenReturn(List.of(
                        rootCause(1L, 1, "cinema-2"),
                        rootCause(1L, 2, "cinema-1"),
                        rootCause(2L, 1, "cinema-1")));
        when(cinemaRepository.findAllByCinemaKeyAndStatDateBetweenOrderByStatDateAsc(
                "cinema-1", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)))
                .thenReturn(List.of(cinemaKpi(
                        LocalDate.of(2026, 7, 1), "100000", 4, 8, "0.00", "0.50")));

        AnalyticsResponses.Dashboard dashboard =
                service.dashboard("2026-07-01", "2026-07-02", "cinema-1");

        assertEquals(1, dashboard.insights().size());
        assertEquals(2L, dashboard.insights().getFirst().id());
    }

    private BusinessInsight insight(long id, String title) {
        BusinessInsight value = new BusinessInsight();
        value.setId(id);
        value.setStatDate(LocalDate.of(2026, 7, 1));
        value.setEntityType("SYSTEM");
        value.setEntityKey("ALL");
        value.setSeverity("WARNING");
        value.setCategory("OCCUPANCY");
        value.setTitle(title);
        value.setSummary(title);
        value.setRootCause(title);
        value.setConfidenceScore(new BigDecimal("0.80"));
        value.setResolved(false);
        value.setCreatedAt(Instant.now());
        return value;
    }

    private RootCauseFactor rootCause(long insightId, int rank, String cinemaKey) {
        RootCauseFactor value = new RootCauseFactor();
        value.setInsightId(insightId);
        value.setRankOrder(rank);
        value.setCauseType("CINEMA_LOW_OCCUPANCY");
        value.setDimensionType("CINEMA");
        value.setDimensionKey(cinemaKey);
        value.setContributionScore(new BigDecimal("0.70"));
        return value;
    }

    private CinemaPerformanceDaily cinemaKpi(
            LocalDate date,
            String netRevenue,
            long bookings,
            long tickets,
            String refundRate,
            String occupancyRate) {
        CinemaPerformanceDaily value = new CinemaPerformanceDaily();
        value.setCinemaKey("cinema-1");
        value.setCinemaName("LoraFilm Quận 1");
        value.setStatDate(date);
        value.setGrossRevenue(new BigDecimal(netRevenue));
        value.setDiscountAmount(BigDecimal.ZERO);
        value.setRefundAmount(BigDecimal.ZERO);
        value.setNetRevenue(new BigDecimal(netRevenue));
        value.setBookingCount(bookings);
        value.setTicketCount(tickets);
        value.setAverageBookingValue(new BigDecimal(netRevenue)
                .divide(BigDecimal.valueOf(bookings)));
        value.setRefundRate(new BigDecimal(refundRate));
        value.setOccupancyRate(new BigDecimal(occupancyRate));
        value.setUpdatedAt(Instant.now());
        return value;
    }
}
