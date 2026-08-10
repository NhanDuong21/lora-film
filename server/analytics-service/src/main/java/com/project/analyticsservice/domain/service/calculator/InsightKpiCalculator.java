package com.project.analyticsservice.domain.service.calculator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.domain.service.RootCauseAnalysisService;
import com.project.analyticsservice.entity.AnomalyDetection;
import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.repository.AnomalyDetectionRepository;
import com.project.analyticsservice.repository.BusinessInsightRepository;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Component
@Order(70)
public class InsightKpiCalculator implements KpiCalculator {
    private static final String ANALYSIS_VERSION = "DIAGNOSTIC_V2";

    private final DailyBusinessKpiRepository dailyRepository;
    private final BusinessInsightRepository insightRepository;
    private final AnomalyDetectionRepository anomalyRepository;
    private final RootCauseAnalysisService rootCauseService;
    private final MetricMathService math;
    private final ObjectMapper objectMapper;
    private final BigDecimal revenueDropThreshold;
    private final BigDecimal refundRateThreshold;
    private final BigDecimal lowOccupancyThreshold;

    public InsightKpiCalculator(
            DailyBusinessKpiRepository dailyRepository,
            BusinessInsightRepository insightRepository,
            AnomalyDetectionRepository anomalyRepository,
            RootCauseAnalysisService rootCauseService,
            MetricMathService math,
            ObjectMapper objectMapper,
            @Value("${analytics.insight.revenue-drop-threshold:0.20}") BigDecimal revenueDropThreshold,
            @Value("${analytics.insight.refund-rate-threshold:0.10}") BigDecimal refundRateThreshold,
            @Value("${analytics.insight.low-occupancy-threshold:0.25}") BigDecimal lowOccupancyThreshold) {
        this.dailyRepository = dailyRepository;
        this.insightRepository = insightRepository;
        this.anomalyRepository = anomalyRepository;
        this.rootCauseService = rootCauseService;
        this.math = math;
        this.objectMapper = objectMapper;
        this.revenueDropThreshold = revenueDropThreshold;
        this.refundRateThreshold = refundRateThreshold;
        this.lowOccupancyThreshold = lowOccupancyThreshold;
    }

    @Override
    public String stage() {
        return "INSIGHT_AND_ROOT_CAUSE";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        DailyBusinessKpi current = dailyRepository.findByStatDate(statDate).orElse(null);
        if (current == null) {
            return;
        }
        Instant resolvedAt = Instant.now();
        List<BusinessInsight> existingInsights = insightRepository.findAllByStatDate(statDate);
        existingInsights.forEach(insight -> {
            insight.setResolved(true);
            insight.setResolvedAt(resolvedAt);
        });
        insightRepository.saveAll(existingInsights);

        LocalDate baselineStart = statDate.minusDays(28);
        LocalDate baselineEnd = statDate.minusDays(1);
        List<DailyBusinessKpi> history = dailyRepository
                .findAllByStatDateBetweenOrderByStatDateAsc(baselineStart, baselineEnd);
        List<DailyBusinessKpi> sameWeekday = history.stream()
                .filter(value -> value.getStatDate().getDayOfWeek() == statDate.getDayOfWeek())
                .toList();
        List<DailyBusinessKpi> baseline = sameWeekday.size() >= 3
                ? sameWeekday : history;
        BigDecimal baselineRevenue = average(
                baseline.stream().map(DailyBusinessKpi::getNetRevenue).toList());
        Set<String> generatedCategories = new HashSet<>();

        if (baselineRevenue.signum() > 0) {
            BigDecimal deviation = current.getNetRevenue().subtract(baselineRevenue)
                    .divide(baselineRevenue, 6, RoundingMode.HALF_UP);
            if (deviation.compareTo(revenueDropThreshold.negate()) < 0) {
                create(
                        statDate, "REVENUE_DROP",
                        severity(deviation.abs(), new BigDecimal("0.40")),
                        "Doanh thu giảm so với mức trung bình gần đây",
                        "Doanh thu thuần thấp hơn "
                                + percent(deviation.abs())
                                + " so với mức thường thấy của cùng thứ trong các tuần gần đây.",
                        baselineStart, baselineEnd, baselineRevenue,
                        current.getNetRevenue(), deviation,
                        current.getDataCompleteness(),
                        Map.of(
                                "baselineDays", baseline.size(),
                                "weekdayMatched", sameWeekday.size() >= 3));
                generatedCategories.add("REVENUE_DROP");
            }
        }
        if (current.getRefundRate().compareTo(refundRateThreshold) > 0) {
            BigDecimal deviation = current.getRefundRate().subtract(refundRateThreshold)
                    .divide(refundRateThreshold, 6, RoundingMode.HALF_UP);
            create(
                    statDate, "HIGH_REFUND_RATE",
                    severity(current.getRefundRate(), new BigDecimal("0.20")),
                    "Tỷ lệ hoàn tiền đang cao",
                    "Tỷ lệ hoàn tiền hiện ở mức " + percent(current.getRefundRate())
                            + ", cao hơn ngưỡng vận hành "
                            + percent(refundRateThreshold) + ".",
                    baselineStart, baselineEnd, refundRateThreshold,
                    current.getRefundRate(), deviation,
                    current.getDataCompleteness(), Map.of());
            generatedCategories.add("HIGH_REFUND_RATE");
        }
        if (current.getBookingCount() > 0
                && current.getOccupancyRate().compareTo(lowOccupancyThreshold) < 0) {
            BigDecimal deviation = current.getOccupancyRate().subtract(lowOccupancyThreshold)
                    .divide(lowOccupancyThreshold, 6, RoundingMode.HALF_UP);
            create(
                    statDate, "LOW_OCCUPANCY", "MEDIUM",
                    "Công suất ghế thấp",
                    "Tỷ lệ lấp đầy ghế chỉ đạt " + percent(current.getOccupancyRate())
                            + ", thấp hơn ngưỡng mục tiêu "
                            + percent(lowOccupancyThreshold) + ".",
                    baselineStart, baselineEnd, lowOccupancyThreshold,
                    current.getOccupancyRate(), deviation,
                    current.getDataCompleteness(), Map.of());
            generatedCategories.add("LOW_OCCUPANCY");
        }
        if (current.getDataCompleteness().compareTo(new BigDecimal("0.80")) < 0) {
            BigDecimal expected = new BigDecimal("0.80");
            BigDecimal deviation = current.getDataCompleteness().subtract(expected)
                    .divide(expected, 6, RoundingMode.HALF_UP);
            create(
                    statDate, "LOW_DATA_COMPLETENESS", "HIGH",
                    "Dữ liệu phân tích chưa đầy đủ",
                    "Mức đầy đủ dữ liệu hiện là "
                            + percent(current.getDataCompleteness())
                            + "; kết quả phân tích và dự báo có thể kém tin cậy.",
                    baselineStart, baselineEnd, expected,
                    current.getDataCompleteness(), deviation,
                    BigDecimal.ONE,
                    Map.of("requiredCompleteness", expected));
            generatedCategories.add("LOW_DATA_COMPLETENESS");
        }

        for (AnomalyDetection anomaly : anomalyRepository.findAllByStatDate(statDate).stream()
                .filter(value -> "ACTIVE".equals(value.getStatus())).toList()) {
            String category = category(anomaly);
            if (generatedCategories.contains(category)) {
                insightRepository.findByFingerprint(category + ":SYSTEM:" + statDate)
                        .ifPresent(insight -> link(anomaly, insight));
                continue;
            }
            BusinessInsight insight = create(
                    statDate, category, anomaly.getSeverity(),
                    anomalyTitle(anomaly),
                    anomalySummary(anomaly),
                    baselineStart, baselineEnd, anomaly.getExpectedValue(),
                    anomaly.getActualValue(), anomaly.getDeviationRate(),
                    current.getDataCompleteness(),
                    Map.of(
                            "detectionMethod", anomaly.getDetectionMethod(),
                            "anomalyScore", anomaly.getAnomalyScore()));
            link(anomaly, insight);
            generatedCategories.add(category);
        }
    }

    private BusinessInsight create(
            LocalDate date,
            String category,
            String severity,
            String title,
            String summary,
            LocalDate baselineStart,
            LocalDate baselineEnd,
            BigDecimal expected,
            BigDecimal actual,
            BigDecimal deviation,
            BigDecimal confidence,
            Map<String, Object> evidence) {
        String fingerprint = category + ":SYSTEM:" + date;
        BusinessInsight insight = insightRepository.findByFingerprint(fingerprint)
                .orElseGet(BusinessInsight::new);
        insight.setFingerprint(fingerprint);
        insight.setStatDate(date);
        insight.setEntityType("SYSTEM");
        insight.setEntityKey("SYSTEM");
        insight.setSeverity(severity);
        insight.setCategory(category);
        insight.setTitle(title);
        insight.setSummary(summary);
        insight.setRootCause("Đang phân tích các nhóm dữ liệu có ảnh hưởng.");
        insight.setEvidenceJson(json(evidence));
        insight.setBaselineStartDate(baselineStart);
        insight.setBaselineEndDate(baselineEnd);
        insight.setExpectedValue(expected);
        insight.setActualValue(actual);
        insight.setDeviationRate(deviation);
        insight.setAnalysisVersion(ANALYSIS_VERSION);
        insight.setConfidenceScore(confidence.min(BigDecimal.ONE)
                .max(BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP));
        insight.setResolved(false);
        insight.setResolvedAt(null);
        insight = insightRepository.save(insight);
        insight.setRootCause(rootCauseService.analyzeAndStore(insight));
        return insightRepository.save(insight);
    }

    private void link(AnomalyDetection anomaly, BusinessInsight insight) {
        anomaly.setInsightId(insight.getId());
        anomalyRepository.save(anomaly);
    }

    private String category(AnomalyDetection anomaly) {
        return switch (anomaly.getMetricName()) {
            case "NET_REVENUE" -> anomaly.getDeviationRate().signum() < 0
                    ? "REVENUE_DROP" : "ANOMALY_NET_REVENUE";
            case "REFUND_RATE" -> anomaly.getDeviationRate().signum() > 0
                    ? "HIGH_REFUND_RATE" : "ANOMALY_REFUND_RATE";
            case "OCCUPANCY_RATE" -> anomaly.getDeviationRate().signum() < 0
                    ? "LOW_OCCUPANCY" : "ANOMALY_OCCUPANCY_RATE";
            default -> "ANOMALY_" + anomaly.getMetricName();
        };
    }

    private String anomalyTitle(AnomalyDetection anomaly) {
        return "Phát hiện bất thường ở " + label(anomaly.getMetricName());
    }

    private String anomalySummary(AnomalyDetection anomaly) {
        return "Giá trị thực tế lệch " + percent(anomaly.getDeviationRate().abs())
                + " so với mức trung bình gần đây. Mức độ bất thường là "
                + anomaly.getAnomalyScore().setScale(2, RoundingMode.HALF_UP) + ".";
    }

    private String label(String metric) {
        return switch (metric) {
            case "NET_REVENUE" -> "doanh thu";
            case "TICKET_COUNT" -> "nhu cầu vé";
            case "REFUND_RATE" -> "tỷ lệ hoàn tiền";
            case "OCCUPANCY_RATE" -> "công suất ghế";
            default -> metric;
        };
    }

    private BigDecimal average(List<BigDecimal> values) {
        return values.isEmpty() ? BigDecimal.ZERO
                : math.ratio(values.stream().map(math::zero)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), values.size());
    }

    private String severity(BigDecimal value, BigDecimal criticalThreshold) {
        return value.compareTo(criticalThreshold) >= 0 ? "CRITICAL" : "HIGH";
    }

    private String percent(BigDecimal value) {
        return value.multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize insight evidence", exception);
        }
    }
}
