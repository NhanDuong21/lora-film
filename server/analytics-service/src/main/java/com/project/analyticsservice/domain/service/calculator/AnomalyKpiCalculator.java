package com.project.analyticsservice.domain.service.calculator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.AnomalyDetection;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.repository.AnomalyDetectionRepository;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import org.springframework.beans.factory.annotation.Value;
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
@Order(67)
public class AnomalyKpiCalculator implements KpiCalculator {
    private static final String METHOD = "ROLLING_Z_SCORE_28D_V1";

    private final DailyBusinessKpiRepository dailyRepository;
    private final AnomalyDetectionRepository anomalyRepository;
    private final MetricMathService math;
    private final ObjectMapper objectMapper;
    private final BigDecimal zScoreThreshold;
    private final BigDecimal minimumDeviation;

    public AnomalyKpiCalculator(
            DailyBusinessKpiRepository dailyRepository,
            AnomalyDetectionRepository anomalyRepository,
            MetricMathService math,
            ObjectMapper objectMapper,
            @Value("${analytics.anomaly.z-score-threshold:2.0}") BigDecimal zScoreThreshold,
            @Value("${analytics.anomaly.minimum-deviation:0.20}") BigDecimal minimumDeviation) {
        this.dailyRepository = dailyRepository;
        this.anomalyRepository = anomalyRepository;
        this.math = math;
        this.objectMapper = objectMapper;
        this.zScoreThreshold = zScoreThreshold;
        this.minimumDeviation = minimumDeviation;
    }

    @Override
    public String stage() {
        return "ANOMALY_DETECTION";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        DailyBusinessKpi current = dailyRepository.findByStatDate(statDate).orElse(null);
        if (current == null) {
            return;
        }
        List<AnomalyDetection> existing = anomalyRepository.findAllByStatDate(statDate);
        existing.forEach(value -> {
            value.setStatus("RESOLVED");
            value.setResolvedAt(Instant.now());
            value.setInsightId(null);
        });
        anomalyRepository.saveAll(existing);

        List<DailyBusinessKpi> baseline =
                dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                        statDate.minusDays(28), statDate.minusDays(1));
        if (baseline.size() < 7) {
            return;
        }
        detect(statDate, "NET_REVENUE", current.getNetRevenue(), baseline,
                DailyBusinessKpi::getNetRevenue);
        detect(statDate, "TICKET_COUNT", BigDecimal.valueOf(current.getTicketCount()), baseline,
                value -> BigDecimal.valueOf(value.getTicketCount()));
        detect(statDate, "REFUND_RATE", current.getRefundRate(), baseline,
                DailyBusinessKpi::getRefundRate);
        detect(statDate, "OCCUPANCY_RATE", current.getOccupancyRate(), baseline,
                DailyBusinessKpi::getOccupancyRate);
    }

    private void detect(
            LocalDate statDate,
            String metric,
            BigDecimal actual,
            List<DailyBusinessKpi> baseline,
            Function<DailyBusinessKpi, BigDecimal> extractor) {
        List<BigDecimal> values = baseline.stream().map(extractor).map(math::zero).toList();
        BigDecimal mean = math.ratio(
                values.stream().reduce(BigDecimal.ZERO, BigDecimal::add), values.size());
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value.subtract(mean).doubleValue(), 2))
                .average().orElse(0);
        BigDecimal standardDeviation = BigDecimal.valueOf(Math.sqrt(variance));
        BigDecimal zScore = standardDeviation.signum() == 0
                ? BigDecimal.ZERO
                : actual.subtract(mean).divide(standardDeviation, 6, RoundingMode.HALF_UP);
        BigDecimal deviation = mean.signum() == 0
                ? BigDecimal.ZERO
                : actual.subtract(mean).divide(mean.abs(), 6, RoundingMode.HALF_UP);

        if (zScore.abs().compareTo(zScoreThreshold) < 0
                || deviation.abs().compareTo(minimumDeviation) < 0) {
            return;
        }
        String fingerprint = metric + ":SYSTEM:" + statDate;
        AnomalyDetection anomaly = anomalyRepository.findByFingerprint(fingerprint)
                .orElseGet(AnomalyDetection::new);
        anomaly.setFingerprint(fingerprint);
        anomaly.setStatDate(statDate);
        anomaly.setEntityType("SYSTEM");
        anomaly.setEntityKey("SYSTEM");
        anomaly.setMetricName(metric);
        anomaly.setActualValue(actual.setScale(6, RoundingMode.HALF_UP));
        anomaly.setExpectedValue(mean.setScale(6, RoundingMode.HALF_UP));
        anomaly.setDeviationRate(deviation);
        anomaly.setAnomalyScore(zScore.abs());
        anomaly.setDetectionMethod(METHOD);
        anomaly.setSeverity(zScore.abs().compareTo(new BigDecimal("3")) >= 0
                ? "CRITICAL" : "HIGH");
        anomaly.setStatus("ACTIVE");
        anomaly.setEvidenceJson(evidence(
                baseline.getFirst().getStatDate(),
                baseline.getLast().getStatDate(),
                baseline.size(),
                mean,
                standardDeviation,
                zScore));
        anomaly.setDetectedAt(Instant.now());
        anomaly.setResolvedAt(null);
        anomalyRepository.save(anomaly);
    }

    private String evidence(
            LocalDate baselineStart,
            LocalDate baselineEnd,
            int sampleSize,
            BigDecimal mean,
            BigDecimal standardDeviation,
            BigDecimal zScore) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("baselineStartDate", baselineStart);
        evidence.put("baselineEndDate", baselineEnd);
        evidence.put("sampleSize", sampleSize);
        evidence.put("mean", mean);
        evidence.put("standardDeviation", standardDeviation);
        evidence.put("zScore", zScore);
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize anomaly evidence", exception);
        }
    }
}
