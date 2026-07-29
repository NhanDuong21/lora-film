package com.project.analyticsservice.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.analyticsservice.entity.BusinessInsight;
import com.project.analyticsservice.entity.CinemaPerformanceDaily;
import com.project.analyticsservice.entity.RootCauseFactor;
import com.project.analyticsservice.repository.CinemaPerformanceDailyRepository;
import com.project.analyticsservice.repository.RootCauseFactorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RootCauseAnalysisService {
    private final CinemaPerformanceDailyRepository cinemaRepository;
    private final RootCauseFactorRepository factorRepository;
    private final ObjectMapper objectMapper;

    public RootCauseAnalysisService(
            CinemaPerformanceDailyRepository cinemaRepository,
            RootCauseFactorRepository factorRepository,
            ObjectMapper objectMapper) {
        this.cinemaRepository = cinemaRepository;
        this.factorRepository = factorRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public String analyzeAndStore(BusinessInsight insight) {
        factorRepository.deleteAllByInsightId(insight.getId());
        List<Candidate> candidates = candidates(insight);
        if (candidates.isEmpty()) {
            Candidate fallback = new Candidate(
                    "SYSTEM_METRIC_DEVIATION", "SYSTEM", "SYSTEM",
                    BigDecimal.ONE, Map.of(
                            "category", insight.getCategory(),
                            "actualValue", Objects.toString(insight.getActualValue(), ""),
                            "expectedValue", Objects.toString(insight.getExpectedValue(), "")));
            candidates = List.of(fallback);
        }

        BigDecimal total = candidates.stream().map(Candidate::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Candidate> top = candidates.stream()
                .sorted(Comparator.comparing(Candidate::weight).reversed())
                .limit(3)
                .toList();
        int rank = 1;
        for (Candidate candidate : top) {
            RootCauseFactor factor = new RootCauseFactor();
            factor.setInsightId(insight.getId());
            factor.setRankOrder(rank++);
            factor.setCauseType(candidate.causeType());
            factor.setDimensionType(candidate.dimensionType());
            factor.setDimensionKey(candidate.dimensionKey());
            factor.setContributionScore(total.signum() == 0
                    ? BigDecimal.ZERO.setScale(6)
                    : candidate.weight().divide(total, 6, RoundingMode.HALF_UP));
            factor.setEvidenceJson(json(candidate.evidence()));
            factorRepository.save(factor);
        }
        Candidate primary = top.getFirst();
        String label = Objects.toString(primary.evidence().get("label"), primary.dimensionKey());
        return switch (primary.causeType()) {
            case "CINEMA_REVENUE_DECLINE" ->
                    "Đóng góp lớn nhất đến từ " + label + ", có doanh thu thấp hơn đường cơ sở.";
            case "CINEMA_REFUND_PRESSURE" ->
                    "Áp lực hoàn tiền tập trung nhiều nhất tại " + label + ".";
            case "CINEMA_LOW_OCCUPANCY" ->
                    "Công suất ghế thấp tập trung nhiều nhất tại " + label + ".";
            case "MISSING_EVENT_SNAPSHOT" ->
                    "Event nguồn đang thiếu dimension hoặc snapshot sức chứa cần cho phân tích.";
            default ->
                    "Biến động của chỉ số hệ thống vượt khỏi vùng kỳ vọng của đường cơ sở.";
        };
    }

    private List<Candidate> candidates(BusinessInsight insight) {
        String category = insight.getCategory();
        LocalDate date = insight.getStatDate();
        if ("LOW_DATA_COMPLETENESS".equals(category)) {
            return List.of(new Candidate(
                    "MISSING_EVENT_SNAPSHOT", "EVENT_CONTRACT", "ANALYTICS_INPUT",
                    BigDecimal.ONE, Map.of(
                            "label", "event đầu vào",
                            "requiredFields",
                            List.of("cinema", "customer", "showtimeCapacity", "membershipTier"))));
        }
        List<CinemaPerformanceDaily> current =
                cinemaRepository.findAllByStatDateBetween(date, date);
        if (current.isEmpty()) {
            return List.of();
        }
        if ("REVENUE_DROP".equals(category) || "ANOMALY_NET_REVENUE".equals(category)) {
            List<CinemaPerformanceDaily> baseline =
                    cinemaRepository.findAllByStatDateBetween(date.minusDays(7), date.minusDays(1));
            Map<String, List<CinemaPerformanceDaily>> byCinema = baseline.stream()
                    .collect(Collectors.groupingBy(CinemaPerformanceDaily::getCinemaKey));
            List<Candidate> values = new ArrayList<>();
            for (CinemaPerformanceDaily cinema : current) {
                List<CinemaPerformanceDaily> history =
                        byCinema.getOrDefault(cinema.getCinemaKey(), List.of());
                BigDecimal expected = average(history, CinemaPerformanceDaily::getNetRevenue);
                BigDecimal decline = expected.subtract(cinema.getNetRevenue()).max(BigDecimal.ZERO);
                if (decline.signum() > 0) {
                    values.add(candidate(
                            "CINEMA_REVENUE_DECLINE", cinema, decline,
                            Map.of(
                                    "actualRevenue", cinema.getNetRevenue(),
                                    "baselineRevenue", expected,
                                    "declineValue", decline)));
                }
            }
            return values;
        }
        if ("HIGH_REFUND_RATE".equals(category) || "ANOMALY_REFUND_RATE".equals(category)) {
            return current.stream()
                    .filter(value -> value.getRefundRate().signum() > 0)
                    .map(value -> candidate(
                            "CINEMA_REFUND_PRESSURE", value, value.getRefundRate(),
                            Map.of("refundRate", value.getRefundRate())))
                    .toList();
        }
        if ("LOW_OCCUPANCY".equals(category) || "ANOMALY_OCCUPANCY_RATE".equals(category)) {
            return current.stream()
                    .map(value -> candidate(
                            "CINEMA_LOW_OCCUPANCY", value,
                            new BigDecimal("0.35").subtract(value.getOccupancyRate())
                                    .max(BigDecimal.ZERO),
                            Map.of("occupancyRate", value.getOccupancyRate())))
                    .filter(value -> value.weight().signum() > 0)
                    .toList();
        }
        return List.of();
    }

    private Candidate candidate(
            String causeType,
            CinemaPerformanceDaily cinema,
            BigDecimal weight,
            Map<String, Object> evidence) {
        Map<String, Object> details = new LinkedHashMap<>(evidence);
        details.put("label", Optional.ofNullable(cinema.getCinemaName())
                .orElse(cinema.getCinemaKey()));
        return new Candidate(
                causeType, "CINEMA", cinema.getCinemaKey(), weight, details);
    }

    private BigDecimal average(
            List<CinemaPerformanceDaily> values,
            Function<CinemaPerformanceDaily, BigDecimal> extractor) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize root-cause evidence", exception);
        }
    }

    private record Candidate(
            String causeType,
            String dimensionType,
            String dimensionKey,
            BigDecimal weight,
            Map<String, Object> evidence) {
    }
}
