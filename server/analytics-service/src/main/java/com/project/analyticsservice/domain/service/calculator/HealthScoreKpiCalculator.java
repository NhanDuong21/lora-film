package com.project.analyticsservice.domain.service.calculator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.AnalyticsHealthScore;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.repository.AnalyticsHealthScoreRepository;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@Order(65)
public class HealthScoreKpiCalculator implements KpiCalculator {
    private static final String ALGORITHM_VERSION = "HEALTH_SCORE_V2";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final DailyBusinessKpiRepository dailyRepository;
    private final AnalyticsHealthScoreRepository healthRepository;
    private final MetricMathService math;
    private final ObjectMapper objectMapper;

    public HealthScoreKpiCalculator(
            DailyBusinessKpiRepository dailyRepository,
            AnalyticsHealthScoreRepository healthRepository,
            MetricMathService math,
            ObjectMapper objectMapper) {
        this.dailyRepository = dailyRepository;
        this.healthRepository = healthRepository;
        this.math = math;
        this.objectMapper = objectMapper;
    }

    @Override
    public String stage() {
        return "HEALTH_SCORE";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        DailyBusinessKpi current = dailyRepository.findByStatDate(statDate).orElse(null);
        if (current == null) {
            return;
        }

        LocalDate baselineStart = statDate.minusDays(28);
        List<DailyBusinessKpi> history =
                dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                        baselineStart, statDate.minusDays(1));
        List<DailyBusinessKpi> sameWeekday = history.stream()
                .filter(value -> value.getStatDate().getDayOfWeek() == statDate.getDayOfWeek())
                .toList();
        List<DailyBusinessKpi> baseline = sameWeekday.size() >= 3
                ? sameWeekday : history;
        BigDecimal baselineRevenue = average(baseline, DailyBusinessKpi::getNetRevenue);
        BigDecimal baselineDemand = average(
                baseline, value -> BigDecimal.valueOf(value.getTicketCount()));

        BigDecimal revenue = performanceScore(current.getNetRevenue(), baselineRevenue);
        BigDecimal demand = performanceScore(
                BigDecimal.valueOf(current.getTicketCount()), baselineDemand);
        BigDecimal occupancy = clamp(math.divide(
                current.getOccupancyRate(), new BigDecimal("0.60"), 6).multiply(HUNDRED));

        long knownCustomers = current.getNewCustomerCount() + current.getReturningCustomerCount();
        BigDecimal customer = knownCustomers == 0
                ? new BigDecimal("50.000000")
                : clamp(math.ratio(current.getReturningCustomerCount(), knownCustomers)
                        .divide(new BigDecimal("0.40"), 6, RoundingMode.HALF_UP)
                        .multiply(HUNDRED));

        BigDecimal refundScore = inverseThresholdScore(
                current.getRefundRate(), new BigDecimal("0.05"), new BigDecimal("0.20"));
        BigDecimal cancellationScore = inverseThresholdScore(
                current.getCancelRate(), new BigDecimal("0.08"), new BigDecimal("0.25"));
        BigDecimal operational = refundScore.multiply(new BigDecimal("0.55"))
                .add(cancellationScore.multiply(new BigDecimal("0.45")));
        BigDecimal dataQuality = clamp(math.zero(current.getDataCompleteness()).multiply(HUNDRED));

        BigDecimal overall = revenue.multiply(new BigDecimal("0.25"))
                .add(demand.multiply(new BigDecimal("0.15")))
                .add(occupancy.multiply(new BigDecimal("0.20")))
                .add(customer.multiply(new BigDecimal("0.10")))
                .add(operational.multiply(new BigDecimal("0.20")))
                .add(dataQuality.multiply(new BigDecimal("0.10")));
        overall = clamp(overall);

        BigDecimal historyConfidence = math.ratio(history.size(), 28).min(BigDecimal.ONE);
        BigDecimal confidence = clamp01(
                historyConfidence.multiply(new BigDecimal("0.65"))
                        .add(math.zero(current.getDataCompleteness())
                                .multiply(new BigDecimal("0.35"))));

        AnalyticsHealthScore score = healthRepository
                .findByEntityTypeAndEntityKeyAndStatDate("SYSTEM", "SYSTEM", statDate)
                .orElseGet(AnalyticsHealthScore::new);
        score.setEntityType("SYSTEM");
        score.setEntityKey("SYSTEM");
        score.setStatDate(statDate);
        score.setOverallScore(scale(overall));
        score.setRevenueScore(scale(revenue));
        score.setDemandScore(scale(demand));
        score.setOccupancyScore(scale(occupancy));
        score.setCustomerScore(scale(customer));
        score.setOperationalScore(scale(operational));
        score.setDataQualityScore(scale(dataQuality));
        score.setHealthStatus(status(overall));
        score.setConfidenceScore(scale(confidence));
        score.setAlgorithmVersion(ALGORITHM_VERSION);
        score.setDriversJson(drivers(
                baselineStart, statDate.minusDays(1), baselineRevenue, baselineDemand,
                current.getRefundRate(), current.getCancelRate(),
                sameWeekday.size() >= 3, history.size()));
        score.setCalculatedAt(Instant.now());
        healthRepository.save(score);
    }

    private BigDecimal average(
            List<DailyBusinessKpi> values,
            Function<DailyBusinessKpi, BigDecimal> extractor) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return math.ratio(
                values.stream().map(extractor).map(math::zero)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                values.size());
    }

    private BigDecimal performanceScore(BigDecimal actual, BigDecimal baseline) {
        if (baseline == null || baseline.signum() == 0) {
            return actual != null && actual.signum() > 0
                    ? new BigDecimal("85.000000") : new BigDecimal("50.000000");
        }
        BigDecimal change = math.divide(
                math.zero(actual).subtract(baseline), baseline.abs(), 6);
        return clamp(new BigDecimal("85").add(change.multiply(HUNDRED)));
    }

    private BigDecimal inverseThresholdScore(
            BigDecimal actual,
            BigDecimal healthyLimit,
            BigDecimal criticalLimit) {
        BigDecimal value = math.zero(actual);
        if (value.compareTo(healthyLimit) <= 0) {
            return HUNDRED.setScale(6);
        }
        if (value.compareTo(criticalLimit) >= 0) {
            return BigDecimal.ZERO.setScale(6);
        }
        return criticalLimit.subtract(value)
                .divide(criticalLimit.subtract(healthyLimit), 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(HUNDRED).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp01(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private String status(BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "HEALTHY";
        }
        if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "STABLE";
        }
        if (score.compareTo(new BigDecimal("40")) >= 0) {
            return "AT_RISK";
        }
        return "CRITICAL";
    }

    private String drivers(
            LocalDate baselineStart,
            LocalDate baselineEnd,
            BigDecimal baselineRevenue,
            BigDecimal baselineDemand,
            BigDecimal refundRate,
            BigDecimal cancellationRate,
            boolean weekdayMatched,
            int historyDays) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("baselineStartDate", baselineStart);
        evidence.put("baselineEndDate", baselineEnd);
        evidence.put("baselineRevenue", baselineRevenue);
        evidence.put("baselineTicketCount", baselineDemand);
        evidence.put("refundRate", refundRate);
        evidence.put("cancellationRate", cancellationRate);
        evidence.put("weekdayMatched", weekdayMatched);
        evidence.put("historyDays", historyDays);
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize health score drivers", exception);
        }
    }
}
