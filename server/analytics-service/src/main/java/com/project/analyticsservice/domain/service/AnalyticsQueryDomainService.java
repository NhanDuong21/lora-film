package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.dto.AnalyticsResponses;
import com.project.analyticsservice.entity.*;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsQueryDomainService {
    private final DailyBusinessKpiRepository dailyRepository;
    private final CinemaPerformanceDailyRepository cinemaRepository;
    private final MoviePerformanceDailyRepository movieRepository;
    private final PromotionPerformanceDailyRepository promotionRepository;
    private final CustomerSegmentDailyRepository segmentRepository;
    private final ForecastResultRepository forecastRepository;
    private final BusinessInsightRepository insightRepository;
    private final RecommendationRepository recommendationRepository;
    private final BusinessAlertRepository alertRepository;
    private final FactBookingMetricRepository bookingFactRepository;
    private final FactBookingCancellationRepository cancellationFactRepository;
    private final FactPaymentRefundRepository refundFactRepository;
    private final ProcessedAnalyticsEventRepository processedEventRepository;
    private final KpiCalculationRunRepository calculationRunRepository;
    private final AnalyticsHealthScoreRepository healthRepository;
    private final AnomalyDetectionRepository anomalyRepository;
    private final RootCauseFactorRepository rootCauseRepository;
    private final ForecastModelMetricRepository forecastMetricRepository;
    private final AnalyticsDataQualityDailyRepository qualityDailyRepository;
    private final MetricMathService math;
    private final FactAnalysisService factAnalysisService;
    private final String timezone;

    public AnalyticsQueryDomainService(
            DailyBusinessKpiRepository dailyRepository,
            CinemaPerformanceDailyRepository cinemaRepository,
            MoviePerformanceDailyRepository movieRepository,
            PromotionPerformanceDailyRepository promotionRepository,
            CustomerSegmentDailyRepository segmentRepository,
            ForecastResultRepository forecastRepository,
            BusinessInsightRepository insightRepository,
            RecommendationRepository recommendationRepository,
            BusinessAlertRepository alertRepository,
            FactBookingMetricRepository bookingFactRepository,
            FactBookingCancellationRepository cancellationFactRepository,
            FactPaymentRefundRepository refundFactRepository,
            ProcessedAnalyticsEventRepository processedEventRepository,
            KpiCalculationRunRepository calculationRunRepository,
            AnalyticsHealthScoreRepository healthRepository,
            AnomalyDetectionRepository anomalyRepository,
            RootCauseFactorRepository rootCauseRepository,
            ForecastModelMetricRepository forecastMetricRepository,
            AnalyticsDataQualityDailyRepository qualityDailyRepository,
            MetricMathService math,
            FactAnalysisService factAnalysisService,
            @Value("${analytics.zone-id:Asia/Ho_Chi_Minh}") String timezone) {
        this.dailyRepository = dailyRepository;
        this.cinemaRepository = cinemaRepository;
        this.movieRepository = movieRepository;
        this.promotionRepository = promotionRepository;
        this.segmentRepository = segmentRepository;
        this.forecastRepository = forecastRepository;
        this.insightRepository = insightRepository;
        this.recommendationRepository = recommendationRepository;
        this.alertRepository = alertRepository;
        this.bookingFactRepository = bookingFactRepository;
        this.cancellationFactRepository = cancellationFactRepository;
        this.refundFactRepository = refundFactRepository;
        this.processedEventRepository = processedEventRepository;
        this.calculationRunRepository = calculationRunRepository;
        this.healthRepository = healthRepository;
        this.anomalyRepository = anomalyRepository;
        this.rootCauseRepository = rootCauseRepository;
        this.forecastMetricRepository = forecastMetricRepository;
        this.qualityDailyRepository = qualityDailyRepository;
        this.math = math;
        this.factAnalysisService = factAnalysisService;
        this.timezone = timezone;
    }

    public AnalyticsResponses.Dashboard dashboard(String start, String end, String cinemaKey) {
        DateRange range = resolveRange(start, end, 30);
        if (StringUtils.hasText(cinemaKey)) {
            return cinemaDashboard(range, normalizeCinemaKey(cinemaKey));
        }
        List<AnalyticsResponses.Insight> insights = insights(range.start(), range.end());
        Set<Long> insightIds = insights.stream().map(AnalyticsResponses.Insight::id).collect(Collectors.toSet());
        return new AnalyticsResponses.Dashboard(
                period(range),
                new AnalyticsResponses.Scope("SYSTEM", null, "Toàn hệ thống"),
                summary(range),
                daily(range.start(), range.end()),
                cinemas(range.start().toString(), range.end().toString(), 8),
                movies(range.start().toString(), range.end().toString(), 10),
                promotions(range.start().toString(), range.end().toString(), 8),
                customerSegments(range.end()),
                forecasts(range.end().plusDays(1), range.end().plusDays(7)),
                healthScore(range.end()),
                anomalies(range.start(), range.end()),
                forecastQuality(range.end()),
                insights,
                recommendations(insightIds),
                alerts(insightIds),
                dataQuality());
    }

    private AnalyticsResponses.Dashboard cinemaDashboard(DateRange range, String cinemaKey) {
        List<CinemaPerformanceDaily> values =
                cinemaRepository.findAllByCinemaKeyAndStatDateBetweenOrderByStatDateAsc(
                        cinemaKey, range.start(), range.end());
        CinemaPerformanceDaily reference = values.stream().findFirst()
                .or(() -> cinemaRepository.findFirstByCinemaKeyOrderByStatDateDesc(cinemaKey))
                .orElseThrow(() -> new BusinessException(
                        "Cinema has no analytics data",
                        "ANALYTICS_CINEMA_NOT_FOUND",
                        HttpStatus.NOT_FOUND));
        String cinemaName = StringUtils.hasText(reference.getCinemaName())
                ? reference.getCinemaName()
                : cinemaKey;
        List<AnalyticsResponses.Insight> scopedInsights = insights(range.start(), range.end())
                .stream()
                .filter(insight -> insight.rootCauses().stream().anyMatch(cause ->
                        "CINEMA".equalsIgnoreCase(cause.dimensionType())
                                && cinemaKey.equals(cause.dimensionKey())))
                .toList();
        Set<Long> insightIds = scopedInsights.stream()
                .map(AnalyticsResponses.Insight::id)
                .collect(Collectors.toSet());
        AnalyticsResponses.CinemaKpi selectedCinema = values.isEmpty()
                ? emptyCinemaKpi(cinemaKey, cinemaName)
                : cinemaResponse(values);
        return new AnalyticsResponses.Dashboard(
                period(range),
                new AnalyticsResponses.Scope("CINEMA", cinemaKey, cinemaName),
                cinemaSummary(values),
                cinemaDaily(values),
                List.of(selectedCinema),
                cinemaMovies(range, cinemaKey, 10),
                cinemaPromotions(range, cinemaKey, 8),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                scopedInsights,
                recommendations(insightIds),
                alerts(insightIds),
                dataQuality());
    }

    public List<AnalyticsResponses.DailyKpi> daily(String start, String end) {
        DateRange range = resolveRange(start, end, 30);
        return dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(range.start(), range.end())
                .stream().map(this::dailyResponse).toList();
    }

    public List<AnalyticsResponses.CinemaKpi> cinemas(String start, String end, Integer limit) {
        DateRange range = resolveRange(start, end, 30);
        int normalizedLimit = normalizeLimit(limit, 10, 100);
        Map<String, List<CinemaPerformanceDaily>> grouped =
                cinemaRepository.findAllByStatDateBetween(range.start(), range.end()).stream()
                        .collect(Collectors.groupingBy(CinemaPerformanceDaily::getCinemaKey));
        return grouped.values().stream().map(this::cinemaResponse)
                .sorted(Comparator.comparing(AnalyticsResponses.CinemaKpi::netRevenue).reversed())
                .limit(normalizedLimit).toList();
    }

    public List<AnalyticsResponses.MovieKpi> movies(String start, String end, Integer limit) {
        DateRange range = resolveRange(start, end, 30);
        int normalizedLimit = normalizeLimit(limit, 10, 100);
        Map<String, List<MoviePerformanceDaily>> grouped =
                movieRepository.findAllByStatDateBetween(range.start(), range.end()).stream()
                        .collect(Collectors.groupingBy(MoviePerformanceDaily::getMovieKey));
        return grouped.values().stream().map(this::movieResponse)
                .sorted(Comparator.comparing(AnalyticsResponses.MovieKpi::netRevenue).reversed())
                .limit(normalizedLimit).toList();
    }

    public List<AnalyticsResponses.PromotionKpi> promotions(String start, String end, Integer limit) {
        DateRange range = resolveRange(start, end, 30);
        int normalizedLimit = normalizeLimit(limit, 10, 100);
        Map<String, List<PromotionPerformanceDaily>> grouped =
                promotionRepository.findAllByStatDateBetween(range.start(), range.end()).stream()
                        .collect(Collectors.groupingBy(PromotionPerformanceDaily::getPromotionKey));
        return grouped.values().stream().map(this::promotionResponse)
                .sorted(Comparator.comparing(AnalyticsResponses.PromotionKpi::generatedRevenue).reversed())
                .limit(normalizedLimit).toList();
    }

    public List<AnalyticsResponses.CustomerSegment> customerSegments(String date) {
        LocalDate resolved = parseDate(date, LocalDate.now());
        return customerSegments(resolved);
    }

    public List<AnalyticsResponses.Forecast> forecasts(String start, String end) {
        DateRange range;
        if (!StringUtils.hasText(start) && !StringUtils.hasText(end)) {
            LocalDate tomorrow = LocalDate.now(ZoneId.of(timezone)).plusDays(1);
            range = new DateRange(tomorrow, tomorrow.plusDays(6));
        } else {
            range = resolveRange(start, end, 7);
        }
        return forecasts(range.start(), range.end());
    }

    public List<AnalyticsResponses.Insight> insights(String start, String end) {
        DateRange range = resolveRange(start, end, 30);
        return insights(range.start(), range.end());
    }

    public List<AnalyticsResponses.Recommendation> recommendations() {
        return recommendations(null);
    }

    public List<AnalyticsResponses.Alert> alerts() {
        return alerts(null);
    }

    public AnalyticsResponses.DataQuality dataQuality() {
        KpiCalculationRun run = calculationRunRepository.findFirstByOrderByStartedAtDesc().orElse(null);
        DailyBusinessKpi latest = dailyRepository
                .findFirstByOrderByStatDateDesc().orElse(null);
        AnalyticsDataQualityDaily quality = qualityDailyRepository
                .findFirstByOrderByStatDateDescCalculatedAtDesc().orElse(null);
        return new AnalyticsResponses.DataQuality(
                bookingFactRepository.count(),
                cancellationFactRepository.count(),
                refundFactRepository.count(),
                processedEventRepository.count(),
                latest == null ? BigDecimal.ZERO : latest.getDataCompleteness(),
                quality == null ? "NO_DATA" : quality.getFreshnessStatus(),
                run == null ? "NEVER_RUN" : run.getStatus(),
                run == null ? null : run.getStatDate(),
                run == null ? null : Optional.ofNullable(run.getCompletedAt()).orElse(run.getStartedAt()));
    }

    private AnalyticsResponses.Summary summary(DateRange range) {
        List<DailyBusinessKpi> values =
                dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(range.start(), range.end());
        BigDecimal gross = sum(values, DailyBusinessKpi::getGrossRevenue);
        BigDecimal discount = sum(values, DailyBusinessKpi::getDiscountAmount);
        BigDecimal refund = sum(values, DailyBusinessKpi::getRefundAmount);
        BigDecimal net = sum(values, DailyBusinessKpi::getNetRevenue);
        long bookings = values.stream().mapToLong(DailyBusinessKpi::getBookingCount).sum();
        long refundBookings = values.stream().mapToLong(DailyBusinessKpi::getRefundBookingCount).sum();
        long cancelled = values.stream().mapToLong(DailyBusinessKpi::getCancelledBookingCount).sum();
        long tickets = values.stream().mapToLong(DailyBusinessKpi::getTicketCount).sum();
        long promotedBookings = values.stream()
                .mapToLong(kpi -> estimatedCount(kpi.getPromotionUsageRate(), kpi.getBookingCount()))
                .sum();
        BigDecimal occupancy = values.isEmpty() ? BigDecimal.ZERO
                : math.ratio(sum(values, DailyBusinessKpi::getOccupancyRate), values.size());
        return new AnalyticsResponses.Summary(
                math.money(gross), math.money(discount), math.money(refund), math.money(net),
                bookings, refundBookings, cancelled, tickets,
                math.money(math.ratio(net, bookings)),
                math.ratio(refundBookings, bookings),
                occupancy,
                math.ratio(promotedBookings, bookings),
                "VND");
    }

    private AnalyticsResponses.Summary cinemaSummary(List<CinemaPerformanceDaily> values) {
        BigDecimal gross = sum(values, CinemaPerformanceDaily::getGrossRevenue);
        BigDecimal discount = sum(values, CinemaPerformanceDaily::getDiscountAmount);
        BigDecimal refund = sum(values, CinemaPerformanceDaily::getRefundAmount);
        BigDecimal net = sum(values, CinemaPerformanceDaily::getNetRevenue);
        long bookings = values.stream().mapToLong(CinemaPerformanceDaily::getBookingCount).sum();
        long refundBookings = values.stream().mapToLong(value ->
                estimatedCount(value.getRefundRate(), value.getBookingCount())).sum();
        long tickets = values.stream().mapToLong(CinemaPerformanceDaily::getTicketCount).sum();
        BigDecimal occupancy = values.isEmpty()
                ? BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP)
                : math.ratio(sum(values, CinemaPerformanceDaily::getOccupancyRate), values.size());
        return new AnalyticsResponses.Summary(
                math.money(gross),
                math.money(discount),
                math.money(refund),
                math.money(net),
                bookings,
                refundBookings,
                0,
                tickets,
                math.money(math.ratio(net, bookings)),
                math.ratio(refundBookings, bookings),
                occupancy,
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                "VND");
    }

    private List<AnalyticsResponses.DailyKpi> daily(LocalDate start, LocalDate end) {
        return dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(start, end)
                .stream().map(this::dailyResponse).toList();
    }

    private List<AnalyticsResponses.DailyKpi> cinemaDaily(
            List<CinemaPerformanceDaily> values) {
        return values.stream().map(value -> new AnalyticsResponses.DailyKpi(
                value.getStatDate(),
                value.getGrossRevenue(),
                value.getDiscountAmount(),
                value.getRefundAmount(),
                value.getNetRevenue(),
                value.getBookingCount(),
                estimatedCount(value.getRefundRate(), value.getBookingCount()),
                0,
                value.getTicketCount(),
                0,
                0,
                value.getAverageBookingValue(),
                value.getRefundRate(),
                value.getOccupancyRate(),
                BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP),
                BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP),
                value.getUpdatedAt())).toList();
    }

    private AnalyticsResponses.DailyKpi dailyResponse(DailyBusinessKpi value) {
        return new AnalyticsResponses.DailyKpi(
                value.getStatDate(), value.getGrossRevenue(), value.getDiscountAmount(),
                value.getRefundAmount(), value.getNetRevenue(), value.getBookingCount(),
                value.getRefundBookingCount(), value.getCancelledBookingCount(),
                value.getTicketCount(), value.getNewCustomerCount(),
                value.getReturningCustomerCount(), value.getAverageBookingValue(),
                value.getRefundRate(), value.getOccupancyRate(),
                value.getPromotionUsageRate(), value.getDataCompleteness(), value.getUpdatedAt());
    }

    private AnalyticsResponses.CinemaKpi cinemaResponse(List<CinemaPerformanceDaily> values) {
        CinemaPerformanceDaily first = values.getFirst();
        BigDecimal net = sum(values, CinemaPerformanceDaily::getNetRevenue);
        long bookings = values.stream().mapToLong(CinemaPerformanceDaily::getBookingCount).sum();
        long refundBookings = values.stream()
                .mapToLong(kpi -> estimatedCount(kpi.getRefundRate(), kpi.getBookingCount())).sum();
        return new AnalyticsResponses.CinemaKpi(
                first.getCinemaKey(), first.getCinemaName(),
                sum(values, CinemaPerformanceDaily::getGrossRevenue),
                sum(values, CinemaPerformanceDaily::getDiscountAmount),
                sum(values, CinemaPerformanceDaily::getRefundAmount),
                net, bookings,
                values.stream().mapToLong(CinemaPerformanceDaily::getTicketCount).sum(),
                math.money(math.ratio(net, bookings)),
                math.ratio(refundBookings, bookings),
                math.ratio(sum(values, CinemaPerformanceDaily::getOccupancyRate), values.size()));
    }

    private AnalyticsResponses.CinemaKpi emptyCinemaKpi(String cinemaKey, String cinemaName) {
        BigDecimal moneyZero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratioZero = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        return new AnalyticsResponses.CinemaKpi(
                cinemaKey, cinemaName, moneyZero, moneyZero, moneyZero, moneyZero,
                0, 0, moneyZero, ratioZero, ratioZero);
    }

    private List<AnalyticsResponses.MovieKpi> cinemaMovies(
            DateRange range, String cinemaKey, int limit) {
        List<FactBookingMetric> facts =
                bookingFactRepository.findAllByCinemaPublicIdAndBusinessDateBetween(
                        cinemaKey, range.start(), range.end());
        Map<String, BigDecimal> refundsByBooking = refundsByBooking(range);
        return facts.stream()
                .filter(value -> StringUtils.hasText(value.getMovieKey()))
                .collect(Collectors.groupingBy(
                        FactBookingMetric::getMovieKey,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values().stream()
                .map(values -> movieResponse(values, refundsByBooking))
                .sorted(Comparator.comparing(AnalyticsResponses.MovieKpi::netRevenue).reversed())
                .limit(limit)
                .toList();
    }

    private AnalyticsResponses.MovieKpi movieResponse(
            List<FactBookingMetric> values,
            Map<String, BigDecimal> refundsByBooking) {
        FactBookingMetric first = values.getFirst();
        Set<String> bookingKeys = values.stream()
                .map(FactBookingMetric::getBookingPublicId)
                .collect(Collectors.toSet());
        BigDecimal gross = sum(values, FactBookingMetric::getGrossAmount);
        BigDecimal discount = sum(values, FactBookingMetric::getDiscountAmount);
        BigDecimal refund = math.sum(bookingKeys.stream()
                .map(key -> refundsByBooking.getOrDefault(key, BigDecimal.ZERO))
                .toList());
        long refundBookings = bookingKeys.stream()
                .filter(key -> refundsByBooking.getOrDefault(key, BigDecimal.ZERO).signum() > 0)
                .count();
        long bookings = bookingKeys.size();
        return new AnalyticsResponses.MovieKpi(
                first.getMovieKey(),
                first.getMovieId(),
                first.getMovieTitle(),
                math.money(gross),
                math.money(discount),
                math.money(refund),
                math.money(gross.subtract(discount).subtract(refund)),
                bookings,
                values.stream().mapToLong(FactBookingMetric::getTicketCount).sum(),
                math.ratio(refundBookings, bookings),
                factAnalysisService.occupancyRate(values));
    }

    private List<AnalyticsResponses.PromotionKpi> cinemaPromotions(
            DateRange range, String cinemaKey, int limit) {
        return bookingFactRepository.findAllByCinemaPublicIdAndBusinessDateBetween(
                        cinemaKey, range.start(), range.end()).stream()
                .filter(value -> StringUtils.hasText(value.getPromotionPublicId()))
                .collect(Collectors.groupingBy(
                        FactBookingMetric::getPromotionPublicId,
                        LinkedHashMap::new,
                        Collectors.toList()))
                .values().stream()
                .map(values -> {
                    FactBookingMetric first = values.getFirst();
                    BigDecimal discount = sum(values, FactBookingMetric::getDiscountAmount);
                    BigDecimal revenue = sum(values, FactBookingMetric::getNetRevenue);
                    return new AnalyticsResponses.PromotionKpi(
                            first.getPromotionPublicId(),
                            StringUtils.hasText(first.getPromotionName())
                                    ? first.getPromotionName()
                                    : first.getPromotionPublicId(),
                            values.stream().map(FactBookingMetric::getBookingPublicId).distinct().count(),
                            math.money(discount),
                            math.money(revenue),
                            math.divide(revenue, discount, 6));
                })
                .sorted(Comparator.comparing(
                        AnalyticsResponses.PromotionKpi::generatedRevenue).reversed())
                .limit(limit)
                .toList();
    }

    private Map<String, BigDecimal> refundsByBooking(DateRange range) {
        return refundFactRepository.findAllByRefundDateBetween(range.start(), range.end())
                .stream()
                .collect(Collectors.toMap(
                        FactPaymentRefund::getBookingPublicId,
                        FactPaymentRefund::getRefundAmount,
                        BigDecimal::add));
    }

    private AnalyticsResponses.MovieKpi movieResponse(List<MoviePerformanceDaily> values) {
        MoviePerformanceDaily first = values.getFirst();
        long bookings = values.stream().mapToLong(MoviePerformanceDaily::getBookingCount).sum();
        long refundBookings = values.stream()
                .mapToLong(kpi -> estimatedCount(kpi.getRefundRate(), kpi.getBookingCount())).sum();
        return new AnalyticsResponses.MovieKpi(
                first.getMovieKey(), first.getMovieId(), first.getMovieTitle(),
                sum(values, MoviePerformanceDaily::getGrossRevenue),
                sum(values, MoviePerformanceDaily::getDiscountAmount),
                sum(values, MoviePerformanceDaily::getRefundAmount),
                sum(values, MoviePerformanceDaily::getNetRevenue),
                bookings,
                values.stream().mapToLong(MoviePerformanceDaily::getTicketCount).sum(),
                math.ratio(refundBookings, bookings),
                math.ratio(sum(values, MoviePerformanceDaily::getOccupancyRate), values.size()));
    }

    private AnalyticsResponses.PromotionKpi promotionResponse(List<PromotionPerformanceDaily> values) {
        PromotionPerformanceDaily first = values.getFirst();
        BigDecimal discount = sum(values, PromotionPerformanceDaily::getDiscountCost);
        BigDecimal generated = sum(values, PromotionPerformanceDaily::getGeneratedRevenue);
        return new AnalyticsResponses.PromotionKpi(
                first.getPromotionKey(), first.getPromotionName(),
                values.stream().mapToLong(PromotionPerformanceDaily::getUsageCount).sum(),
                discount, generated, math.divide(generated, discount, 6));
    }

    private List<AnalyticsResponses.CustomerSegment> customerSegments(LocalDate date) {
        LocalDate latestDate = segmentRepository
                .findFirstByStatDateLessThanEqualOrderByStatDateDesc(date)
                .map(CustomerSegmentDaily::getStatDate)
                .orElse(date);
        return segmentRepository.findAllByStatDate(latestDate).stream()
                .map(value -> new AnalyticsResponses.CustomerSegment(
                        value.getStatDate(), value.getMembershipTier(), value.getActiveUsers(),
                        value.getNewUsers(), value.getReturningUsers(), value.getTotalSpending(),
                        value.getAverageSpending(), value.getCustomerLifetimeValue()))
                .sorted(Comparator.comparing(AnalyticsResponses.CustomerSegment::customerLifetimeValue).reversed())
                .toList();
    }

    private List<AnalyticsResponses.Forecast> forecasts(LocalDate start, LocalDate end) {
        return forecastRepository.findAllByForecastDateBetweenOrderByForecastDateAsc(start, end)
                .stream().map(value -> new AnalyticsResponses.Forecast(
                        value.getEntityType(), value.getEntityKey(), value.getForecastDate(),
                        value.getForecastType(), value.getAsOfDate(), value.getPredictedValue(),
                        value.getPredictionLowerBound(), value.getPredictionUpperBound(),
                        value.getConfidenceScore(), value.getAlgorithm(), value.getModelVersion(),
                        value.getTrainingStartDate(), value.getTrainingEndDate(), value.getGeneratedAt()))
                .toList();
    }

    private List<AnalyticsResponses.Insight> insights(LocalDate start, LocalDate end) {
        List<BusinessInsight> values =
                insightRepository.findAllByResolvedFalseAndStatDateBetweenOrderByCreatedAtDesc(start, end);
        Map<Long, List<RootCauseFactor>> rootCauses = values.isEmpty()
                ? Map.of()
                : rootCauseRepository.findAllByInsightIdInOrderByInsightIdAscRankOrderAsc(
                                values.stream().map(BusinessInsight::getId).toList())
                        .stream().collect(Collectors.groupingBy(RootCauseFactor::getInsightId));
        return values.stream().map(value -> new AnalyticsResponses.Insight(
                        value.getId(), value.getStatDate(), value.getEntityType(),
                        value.getEntityKey(), value.getSeverity(), value.getCategory(),
                        value.getTitle(), value.getSummary(), value.getRootCause(),
                        value.getEvidenceJson(), value.getBaselineStartDate(),
                        value.getBaselineEndDate(), value.getExpectedValue(),
                        value.getActualValue(), value.getDeviationRate(),
                        value.getAnalysisVersion(), value.getConfidenceScore(),
                        rootCauses.getOrDefault(value.getId(), List.of()).stream()
                                .map(factor -> new AnalyticsResponses.RootCause(
                                        factor.getRankOrder(), factor.getCauseType(),
                                        factor.getDimensionType(), factor.getDimensionKey(),
                                        factor.getContributionScore(), factor.getEvidenceJson()))
                                .toList(),
                        value.getCreatedAt())).toList();
    }

    private List<AnalyticsResponses.Recommendation> recommendations(Set<Long> insightIds) {
        Set<Long> activeInsightIds = insightIds == null
                ? insightRepository.findAllByResolvedFalse().stream()
                    .map(BusinessInsight::getId).collect(Collectors.toSet())
                : insightIds;
        if (activeInsightIds.isEmpty()) {
            return List.of();
        }
        return recommendationRepository
                .findTop100ByInsightIdInOrderByCreatedAtDesc(activeInsightIds).stream()
                .map(value -> new AnalyticsResponses.Recommendation(
                        value.getId(), value.getInsightId(), value.getTargetService(),
                        value.getActionType(), value.getPriority(), value.getTitle(),
                        value.getDescription(), value.getExpectedImpact(),
                        value.getEstimatedImpactValue(), value.getImpactUnit(),
                        value.getConfidenceScore(), value.getStatus(),
                        value.getAcceptedBy(), value.getAcceptedAt(),
                        value.getCompletedAt(), value.getExpiresAt(), value.getCreatedAt()))
                .limit(50).toList();
    }

    private List<AnalyticsResponses.Alert> alerts(Set<Long> insightIds) {
        Set<Long> activeInsightIds = insightIds == null
                ? insightRepository.findAllByResolvedFalse().stream()
                    .map(BusinessInsight::getId).collect(Collectors.toSet())
                : insightIds;
        if (activeInsightIds.isEmpty()) {
            return List.of();
        }
        return alertRepository.findTop100ByInsightIdInOrderByCreatedAtDesc(activeInsightIds).stream()
                .map(value -> new AnalyticsResponses.Alert(
                        value.getId(), value.getInsightId(), value.getEntityType(),
                        value.getEntityKey(), value.getSeverity(), value.getTitle(),
                        value.getMessage(), value.getAcknowledged(),
                        value.getAcknowledgedBy(), value.getAcknowledgedAt(),
                        value.getResolved(), value.getResolvedAt(), value.getCreatedAt()))
                .limit(50).toList();
    }

    public AnalyticsResponses.HealthScore healthScore(String date) {
        return healthScore(parseDate(date, LocalDate.now(ZoneId.of(timezone))));
    }

    public List<AnalyticsResponses.Anomaly> anomalies(String start, String end) {
        DateRange range = resolveRange(start, end, 30);
        return anomalies(range.start(), range.end());
    }

    public List<AnalyticsResponses.ForecastQuality> forecastQuality(String date) {
        return forecastQuality(parseDate(date, LocalDate.now(ZoneId.of(timezone))));
    }

    private AnalyticsResponses.HealthScore healthScore(LocalDate date) {
        return healthRepository.findAllByStatDateBetweenOrderByStatDateDesc(
                        date.minusDays(30), date).stream()
                .filter(value -> "SYSTEM".equals(value.getEntityType())
                        && "SYSTEM".equals(value.getEntityKey()))
                .findFirst()
                .map(value -> new AnalyticsResponses.HealthScore(
                        value.getStatDate(), value.getOverallScore(), value.getRevenueScore(),
                        value.getDemandScore(), value.getOccupancyScore(), value.getCustomerScore(),
                        value.getOperationalScore(), value.getDataQualityScore(),
                        value.getHealthStatus(), value.getConfidenceScore(),
                        value.getAlgorithmVersion(), value.getDriversJson(),
                        value.getCalculatedAt()))
                .orElse(null);
    }

    private List<AnalyticsResponses.Anomaly> anomalies(LocalDate start, LocalDate end) {
        return anomalyRepository
                .findAllByStatusAndStatDateBetweenOrderByDetectedAtDesc("ACTIVE", start, end)
                .stream().map(value -> new AnalyticsResponses.Anomaly(
                        value.getId(), value.getInsightId(), value.getStatDate(),
                        value.getMetricName(), value.getActualValue(), value.getExpectedValue(),
                        value.getDeviationRate(), value.getAnomalyScore(),
                        value.getDetectionMethod(), value.getSeverity(), value.getStatus(),
                        value.getEvidenceJson(), value.getDetectedAt()))
                .limit(50).toList();
    }

    private List<AnalyticsResponses.ForecastQuality> forecastQuality(LocalDate date) {
        Map<String, ForecastModelMetric> latestByType = new LinkedHashMap<>();
        forecastMetricRepository
                .findAllByEvaluationDateBetweenOrderByEvaluationDateDesc(date.minusDays(30), date)
                .forEach(value -> latestByType.putIfAbsent(value.getForecastType(), value));
        return latestByType.values().stream().map(value ->
                new AnalyticsResponses.ForecastQuality(
                        value.getForecastType(), value.getAlgorithm(), value.getModelVersion(),
                        value.getEvaluationDate(), value.getTestStartDate(),
                        value.getTestEndDate(), value.getSampleSize(), value.getMae(),
                        value.getRmse(), value.getMape(), value.getBias(),
                        value.getCalculatedAt())).toList();
    }

    private <T> BigDecimal sum(List<T> values, Function<T, BigDecimal> extractor) {
        return math.sum(values.stream().map(extractor).toList());
    }

    private long estimatedCount(BigDecimal rate, long total) {
        return math.zero(rate)
                .multiply(BigDecimal.valueOf(total))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private String normalizeCinemaKey(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw invalid(
                    "cinemaKey must contain between 1 and 100 characters",
                    "ANALYTICS_INVALID_CINEMA_KEY");
        }
        return normalized;
    }

    private int normalizeLimit(Integer value, int fallback, int max) {
        int resolved = value == null ? fallback : value;
        if (resolved < 1 || resolved > max) {
            throw invalid("limit must be between 1 and " + max, "ANALYTICS_INVALID_LIMIT");
        }
        return resolved;
    }

    private DateRange resolveRange(String start, String end, int defaultDays) {
        boolean hasStart = StringUtils.hasText(start);
        boolean hasEnd = StringUtils.hasText(end);
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        LocalDate endDate = hasEnd ? parseDate(end, null) : today;
        LocalDate startDate = hasStart ? parseDate(start, null) : endDate.minusDays(defaultDays - 1L);
        if (hasStart != hasEnd) {
            throw invalid("startDate and endDate must be provided together",
                    "ANALYTICS_INVALID_DATE_RANGE");
        }
        if (startDate.isAfter(endDate) || ChronoUnit.DAYS.between(startDate, endDate) > 366) {
            throw invalid("Date range must be ordered and no longer than 367 days",
                    "ANALYTICS_INVALID_DATE_RANGE");
        }
        return new DateRange(startDate, endDate);
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw invalid("Date must use ISO format yyyy-MM-dd", "ANALYTICS_INVALID_DATE_FORMAT");
        }
    }

    private BusinessException invalid(String message, String code) {
        return new BusinessException(message, code, HttpStatus.BAD_REQUEST);
    }

    private AnalyticsResponses.Period period(DateRange range) {
        return new AnalyticsResponses.Period(range.start(), range.end(), timezone);
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
