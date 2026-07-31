package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.entity.ForecastModelMetric;
import com.project.analyticsservice.entity.ForecastResult;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import com.project.analyticsservice.repository.ForecastModelMetricRepository;
import com.project.analyticsservice.repository.ForecastResultRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
@Order(60)
public class ForecastKpiCalculator implements KpiCalculator {
    static final String ALGORITHM = "HYBRID_WEEKDAY_RECENCY_DAMPED_TREND";
    static final String MODEL_VERSION = "3.0";

    private final DailyBusinessKpiRepository dailyRepository;
    private final ForecastResultRepository forecastRepository;
    private final ForecastModelMetricRepository modelMetricRepository;
    private final MetricMathService math;
    private final int horizonDays;

    public ForecastKpiCalculator(
            DailyBusinessKpiRepository dailyRepository,
            ForecastResultRepository forecastRepository,
            ForecastModelMetricRepository modelMetricRepository,
            MetricMathService math,
            @Value("${analytics.forecast.horizon-days:7}") int horizonDays) {
        this.dailyRepository = dailyRepository;
        this.forecastRepository = forecastRepository;
        this.modelMetricRepository = modelMetricRepository;
        this.math = math;
        this.horizonDays = Math.max(1, Math.min(horizonDays, 30));
    }

    @Override
    public String stage() {
        return "FORECAST";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        LocalDate trainingStart = statDate.minusDays(83);
        List<DailyBusinessKpi> history =
                dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(trainingStart, statDate);
        if (history.size() < 14) {
            return;
        }
        trainingStart = history.getFirst().getStatDate();
        BigDecimal completeness = average(history, DailyBusinessKpi::getDataCompleteness);

        for (int day = 1; day <= horizonDays; day++) {
            LocalDate forecastDate = statDate.plusDays(day);
            upsertForecast("REVENUE", forecastDate,
                    prediction(history, forecastDate, DailyBusinessKpi::getNetRevenue),
                    completeness, trainingStart, statDate, true);
            upsertForecast("TICKET", forecastDate,
                    prediction(history, forecastDate,
                            value -> BigDecimal.valueOf(value.getTicketCount())),
                    completeness, trainingStart, statDate, false);
            upsertForecast("OCCUPANCY", forecastDate,
                    prediction(history, forecastDate, DailyBusinessKpi::getOccupancyRate),
                    completeness, trainingStart, statDate, false);
        }

        backtest(statDate, "REVENUE", DailyBusinessKpi::getNetRevenue);
        backtest(statDate, "TICKET",
                value -> BigDecimal.valueOf(value.getTicketCount()));
        backtest(statDate, "OCCUPANCY", DailyBusinessKpi::getOccupancyRate);
    }

    private Prediction prediction(
            List<DailyBusinessKpi> history,
            LocalDate targetDate,
            Function<DailyBusinessKpi, BigDecimal> extractor) {
        List<DailyBusinessKpi> seasonal = history.stream()
                .filter(value -> value.getStatDate().getDayOfWeek() == targetDate.getDayOfWeek())
                .skip(Math.max(0, history.stream()
                        .filter(value -> value.getStatDate().getDayOfWeek() == targetDate.getDayOfWeek())
                        .count() - 8))
                .toList();
        List<DailyBusinessKpi> recent = history.stream()
                .skip(Math.max(0, history.size() - 21L))
                .toList();
        BigDecimal recentLevel = weightedAverage(recent, extractor);
        BigDecimal seasonalLevel = seasonal.size() >= 3
                ? weightedAverage(seasonal, extractor)
                : recentLevel;
        BigDecimal trendLevel = dampedTrend(recent, targetDate, extractor, recentLevel);

        BigDecimal seasonalWeight = seasonal.size() >= 3
                ? new BigDecimal("0.55") : BigDecimal.ZERO;
        BigDecimal recentWeight = seasonal.size() >= 3
                ? new BigDecimal("0.30") : new BigDecimal("0.70");
        BigDecimal trendWeight = seasonal.size() >= 3
                ? new BigDecimal("0.15") : new BigDecimal("0.30");
        BigDecimal predicted = seasonalLevel.multiply(seasonalWeight)
                .add(recentLevel.multiply(recentWeight))
                .add(trendLevel.multiply(trendWeight))
                .max(BigDecimal.ZERO);

        List<BigDecimal> recentValues = recent.stream()
                .map(extractor)
                .map(math::zero)
                .toList();
        BigDecimal center = median(recentValues);
        BigDecimal mad = median(recentValues.stream()
                .map(value -> value.subtract(center).abs())
                .toList());
        BigDecimal interval = mad.multiply(new BigDecimal("1.4826"))
                .multiply(new BigDecimal("1.645"))
                .max(predicted.abs().multiply(new BigDecimal("0.08")));
        BigDecimal seasonalCoverage = math.ratio(seasonal.size(), 6).min(BigDecimal.ONE);
        BigDecimal historyCoverage = math.ratio(history.size(), 42).min(BigDecimal.ONE);
        BigDecimal coverage = seasonalCoverage.multiply(new BigDecimal("0.60"))
                .add(historyCoverage.multiply(new BigDecimal("0.40")));
        BigDecimal disagreement = maxDifference(
                predicted, List.of(seasonalLevel, recentLevel, trendLevel));
        BigDecimal agreement = predicted.signum() == 0
                ? BigDecimal.ONE
                : BigDecimal.ONE.subtract(disagreement
                        .divide(predicted.abs(), 6, RoundingMode.HALF_UP)
                        .min(BigDecimal.ONE));
        return new Prediction(predicted, interval, coverage, agreement);
    }

    private void upsertForecast(
            String type,
            LocalDate forecastDate,
            Prediction prediction,
            BigDecimal completeness,
            LocalDate trainingStart,
            LocalDate trainingEnd,
            boolean money) {
        BigDecimal confidence = prediction.coverage().multiply(new BigDecimal("0.45"))
                .add(completeness.multiply(new BigDecimal("0.35")))
                .add(prediction.agreement().multiply(new BigDecimal("0.20")))
                .min(new BigDecimal("0.950000"));
        BigDecimal rawPrediction = "OCCUPANCY".equals(type)
                ? prediction.value().min(BigDecimal.ONE)
                : prediction.value();
        BigDecimal predicted = money
                ? math.money(rawPrediction)
                : "TICKET".equals(type)
                    ? rawPrediction.setScale(0, RoundingMode.HALF_UP)
                    : rawPrediction.setScale(6, RoundingMode.HALF_UP);
        BigDecimal lower = rawPrediction.subtract(prediction.interval())
                .max(BigDecimal.ZERO);
        BigDecimal upper = rawPrediction.add(prediction.interval());
        if ("OCCUPANCY".equals(type)) {
            upper = upper.min(BigDecimal.ONE);
        }

        ForecastResult forecast = forecastRepository
                .findByEntityTypeAndEntityKeyAndForecastDateAndForecastType(
                        "SYSTEM", "SYSTEM", forecastDate, type)
                .orElseGet(ForecastResult::new);
        forecast.setEntityType("SYSTEM");
        forecast.setEntityKey("SYSTEM");
        forecast.setForecastDate(forecastDate);
        forecast.setForecastType(type);
        forecast.setAsOfDate(trainingEnd);
        forecast.setPredictedValue(predicted);
        forecast.setPredictionLowerBound(lower.setScale(6, RoundingMode.HALF_UP));
        forecast.setPredictionUpperBound(upper.setScale(6, RoundingMode.HALF_UP));
        forecast.setConfidenceScore(confidence.setScale(6, RoundingMode.HALF_UP));
        forecast.setAlgorithm(ALGORITHM);
        forecast.setModelVersion(MODEL_VERSION);
        forecast.setTrainingStartDate(trainingStart);
        forecast.setTrainingEndDate(trainingEnd);
        forecastRepository.save(forecast);
    }

    private void backtest(
            LocalDate evaluationDate,
            String type,
            Function<DailyBusinessKpi, BigDecimal> extractor) {
        LocalDate testStart = evaluationDate.minusDays(13);
        List<DailyBusinessKpi> completeHistory =
                dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(
                        evaluationDate.minusDays(104), evaluationDate);
        List<BigDecimal> errors = new ArrayList<>();
        BigDecimal absoluteActualTotal = BigDecimal.ZERO;

        for (DailyBusinessKpi actualKpi : completeHistory) {
            if (actualKpi.getStatDate().isBefore(testStart)) {
                continue;
            }
            List<DailyBusinessKpi> training = completeHistory.stream()
                    .filter(value -> value.getStatDate().isBefore(actualKpi.getStatDate()))
                    .filter(value -> !value.getStatDate().isBefore(actualKpi.getStatDate().minusDays(84)))
                    .toList();
            if (training.size() < 14) {
                continue;
            }
            BigDecimal predicted = prediction(
                    training, actualKpi.getStatDate(), extractor).value();
            BigDecimal actual = math.zero(extractor.apply(actualKpi));
            BigDecimal error = predicted.subtract(actual);
            errors.add(error);
            absoluteActualTotal = absoluteActualTotal.add(actual.abs());
        }
        if (errors.isEmpty()) {
            return;
        }

        BigDecimal mae = averageValues(errors.stream().map(BigDecimal::abs).toList());
        double meanSquared = errors.stream()
                .mapToDouble(value -> Math.pow(value.doubleValue(), 2)).average().orElse(0);
        BigDecimal rmse = BigDecimal.valueOf(Math.sqrt(meanSquared));
        BigDecimal mape = absoluteActualTotal.signum() == 0
                ? null
                : errors.stream().map(BigDecimal::abs)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(absoluteActualTotal, 6, RoundingMode.HALF_UP);
        BigDecimal bias = averageValues(errors);

        ForecastModelMetric metric = modelMetricRepository
                .findByEntityTypeAndEntityKeyAndForecastTypeAndModelVersionAndEvaluationDate(
                        "SYSTEM", "SYSTEM", type, MODEL_VERSION, evaluationDate)
                .orElseGet(ForecastModelMetric::new);
        metric.setEntityType("SYSTEM");
        metric.setEntityKey("SYSTEM");
        metric.setForecastType(type);
        metric.setAlgorithm(ALGORITHM);
        metric.setModelVersion(MODEL_VERSION);
        metric.setEvaluationDate(evaluationDate);
        metric.setTestStartDate(testStart);
        metric.setTestEndDate(evaluationDate);
        metric.setSampleSize(errors.size());
        metric.setMae(mae.setScale(6, RoundingMode.HALF_UP));
        metric.setRmse(rmse.setScale(6, RoundingMode.HALF_UP));
        metric.setMape(mape == null ? null : mape.setScale(6, RoundingMode.HALF_UP));
        metric.setBias(bias.setScale(6, RoundingMode.HALF_UP));
        metric.setCalculatedAt(Instant.now());
        modelMetricRepository.save(metric);
    }

    private BigDecimal average(
            List<DailyBusinessKpi> values,
            Function<DailyBusinessKpi, BigDecimal> extractor) {
        return values.isEmpty() ? BigDecimal.ZERO
                : math.ratio(values.stream().map(extractor).map(math::zero)
                        .reduce(BigDecimal.ZERO, BigDecimal::add), values.size());
    }

    private BigDecimal averageValues(List<BigDecimal> values) {
        return values.isEmpty() ? BigDecimal.ZERO
                : math.ratio(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add), values.size());
    }

    private BigDecimal weightedAverage(
            List<DailyBusinessKpi> values,
            Function<DailyBusinessKpi, BigDecimal> extractor) {
        BigDecimal weightedTotal = BigDecimal.ZERO;
        long totalWeight = 0;
        for (int index = 0; index < values.size(); index++) {
            long weight = index + 1L;
            weightedTotal = weightedTotal.add(
                    math.zero(extractor.apply(values.get(index)))
                            .multiply(BigDecimal.valueOf(weight)));
            totalWeight += weight;
        }
        return math.ratio(weightedTotal, totalWeight);
    }

    private BigDecimal dampedTrend(
            List<DailyBusinessKpi> values,
            LocalDate targetDate,
            Function<DailyBusinessKpi, BigDecimal> extractor,
            BigDecimal recentLevel) {
        if (values.size() < 7) {
            return recentLevel;
        }
        double meanX = (values.size() - 1) / 2.0;
        double meanY = values.stream()
                .map(extractor)
                .map(math::zero)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0);
        double numerator = 0;
        double denominator = 0;
        for (int index = 0; index < values.size(); index++) {
            double centeredX = index - meanX;
            numerator += centeredX
                    * (math.zero(extractor.apply(values.get(index))).doubleValue() - meanY);
            denominator += centeredX * centeredX;
        }
        BigDecimal slope = denominator == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(numerator / denominator);
        BigDecimal maximumDailyChange = recentLevel.abs().multiply(new BigDecimal("0.08"));
        slope = slope.max(maximumDailyChange.negate()).min(maximumDailyChange);
        long daysAhead = Math.max(1,
                java.time.temporal.ChronoUnit.DAYS.between(
                        values.getLast().getStatDate(), targetDate));
        return recentLevel.add(
                slope.multiply(BigDecimal.valueOf(daysAhead))
                        .multiply(new BigDecimal("0.65")))
                .max(BigDecimal.ZERO);
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

    private BigDecimal maxDifference(BigDecimal center, List<BigDecimal> values) {
        return values.stream()
                .map(value -> value.subtract(center).abs())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private record Prediction(
            BigDecimal value,
            BigDecimal interval,
            BigDecimal coverage,
            BigDecimal agreement) {
    }
}
