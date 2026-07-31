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
    private static final String METHOD = "WEEKDAY_ROBUST_MAD_56D_V2";
    private static final BigDecimal MAD_SCALE = new BigDecimal("1.4826");
    private static final BigDecimal MINIMUM_ROBUST_SCORE = new BigDecimal("3.5");

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
            @Value("${analytics.anomaly.z-score-threshold:3.5}") BigDecimal zScoreThreshold,
            @Value("${analytics.anomaly.minimum-deviation:0.20}") BigDecimal minimumDeviation) {
        this.dailyRepository = dailyRepository;
        this.anomalyRepository = anomalyRepository;
        this.math = math;
        this.objectMapper = objectMapper;
        this.zScoreThreshold = zScoreThreshold.max(MINIMUM_ROBUST_SCORE);
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
                        statDate.minusDays(56), statDate.minusDays(1));
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
        List<DailyBusinessKpi> sameWeekday = baseline.stream()
                .filter(value -> value.getStatDate().getDayOfWeek() == statDate.getDayOfWeek())
                .toList();
        List<DailyBusinessKpi> sample = sameWeekday.size() >= 4
                ? sameWeekday
                : baseline.stream().skip(Math.max(0, baseline.size() - 28L)).toList();
        List<BigDecimal> values = sample.stream().map(extractor).map(math::zero).toList();
        BigDecimal expected = median(values);
        BigDecimal mad = median(values.stream()
                .map(value -> value.subtract(expected).abs())
                .toList());
        BigDecimal robustScale = mad.multiply(MAD_SCALE)
                .max(minimumScale(metric, expected));
        BigDecimal robustScore = actual.subtract(expected)
                .divide(robustScale, 6, RoundingMode.HALF_UP);
        BigDecimal deviation = expected.signum() == 0
                ? (actual.signum() == 0 ? BigDecimal.ZERO : BigDecimal.ONE)
                : actual.subtract(expected).divide(expected.abs(), 6, RoundingMode.HALF_UP);

        boolean materialBreak = deviation.abs().compareTo(new BigDecimal("0.50")) >= 0
                && robustScore.abs().compareTo(new BigDecimal("3.0")) >= 0;
        if ((robustScore.abs().compareTo(zScoreThreshold) < 0 && !materialBreak)
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
        anomaly.setExpectedValue(expected.setScale(6, RoundingMode.HALF_UP));
        anomaly.setDeviationRate(deviation);
        anomaly.setAnomalyScore(robustScore.abs());
        anomaly.setDetectionMethod(METHOD);
        anomaly.setSeverity(robustScore.abs().compareTo(new BigDecimal("5")) >= 0
                || deviation.abs().compareTo(new BigDecimal("0.50")) >= 0
                ? "CRITICAL" : "HIGH");
        anomaly.setStatus("ACTIVE");
        anomaly.setEvidenceJson(evidence(
                sample.getFirst().getStatDate(),
                sample.getLast().getStatDate(),
                sample.size(),
                expected,
                mad,
                robustScale,
                robustScore,
                sameWeekday.size() >= 4));
        anomaly.setDetectedAt(Instant.now());
        anomaly.setResolvedAt(null);
        anomalyRepository.save(anomaly);
    }

    private String evidence(
            LocalDate baselineStart,
            LocalDate baselineEnd,
            int sampleSize,
            BigDecimal median,
            BigDecimal mad,
            BigDecimal robustScale,
            BigDecimal robustScore,
            boolean weekdayMatched) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("baselineStartDate", baselineStart);
        evidence.put("baselineEndDate", baselineEnd);
        evidence.put("sampleSize", sampleSize);
        evidence.put("median", median);
        evidence.put("medianAbsoluteDeviation", mad);
        evidence.put("robustScale", robustScale);
        evidence.put("robustScore", robustScore);
        evidence.put("weekdayMatched", weekdayMatched);
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize anomaly evidence", exception);
        }
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return sorted.get(middle - 1).add(sorted.get(middle))
                .divide(new BigDecimal("2"), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal minimumScale(String metric, BigDecimal expected) {
        BigDecimal relativeFloor = expected.abs().multiply(new BigDecimal("0.05"));
        return switch (metric) {
            case "REFUND_RATE" -> relativeFloor.max(new BigDecimal("0.005"));
            case "OCCUPANCY_RATE" -> relativeFloor.max(new BigDecimal("0.02"));
            case "TICKET_COUNT" -> relativeFloor.max(BigDecimal.ONE);
            default -> relativeFloor.max(BigDecimal.ONE);
        };
    }
}
