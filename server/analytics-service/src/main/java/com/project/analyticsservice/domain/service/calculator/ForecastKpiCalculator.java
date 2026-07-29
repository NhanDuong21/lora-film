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
    static final String ALGORITHM = "WEEKDAY_SEASONAL_WEIGHTED_AVERAGE";
    static final String MODEL_VERSION = "2.0";

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
        LocalDate trainingStart = statDate.minusDays(55);
        List<DailyBusinessKpi> history =
                dailyRepository.findAllByStatDateBetweenOrderByStatDateAsc(trainingStart, statDate);
        if (history.isEmpty()) {
            return;
        }
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
                .toList();
        List<DailyBusinessKpi> sample = seasonal.size() >= 2
                ? seasonal : history.stream().skip(Math.max(0, history.size() - 28L)).toList();

        BigDecimal weightedTotal = BigDecimal.ZERO;
        long totalWeight = 0;
        for (int index = 0; index < sample.size(); index++) {
            long weight = index + 1L;
            weightedTotal = weightedTotal.add(
                    math.zero(extractor.apply(sample.get(index)))
                            .multiply(BigDecimal.valueOf(weight)));
            totalWeight += weight;
        }
        BigDecimal predicted = math.ratio(weightedTotal, totalWeight);
        double variance = sample.stream()
                .map(extractor)
                .map(math::zero)
                .mapToDouble(value -> Math.pow(value.subtract(predicted).doubleValue(), 2))
                .average().orElse(0);
        BigDecimal interval = BigDecimal.valueOf(Math.sqrt(variance))
                .multiply(new BigDecimal("1.96"));
        BigDecimal coverage = math.ratio(sample.size(), 8).min(BigDecimal.ONE);
        return new Prediction(predicted, interval, coverage);
    }

    private void upsertForecast(
            String type,
            LocalDate forecastDate,
            Prediction prediction,
            BigDecimal completeness,
            LocalDate trainingStart,
            LocalDate trainingEnd,
            boolean money) {
        BigDecimal confidence = prediction.coverage().multiply(new BigDecimal("0.60"))
                .add(completeness.multiply(new BigDecimal("0.40")))
                .min(new BigDecimal("0.980000"));
        BigDecimal predicted = money
                ? math.money(prediction.value())
                : "TICKET".equals(type)
                    ? prediction.value().setScale(0, RoundingMode.HALF_UP)
                    : prediction.value().setScale(6, RoundingMode.HALF_UP);
        BigDecimal lower = prediction.value().subtract(prediction.interval())
                .max(BigDecimal.ZERO);
        BigDecimal upper = prediction.value().add(prediction.interval());

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
                        evaluationDate.minusDays(69), evaluationDate);
        List<BigDecimal> errors = new ArrayList<>();
        List<BigDecimal> percentageErrors = new ArrayList<>();

        for (DailyBusinessKpi actualKpi : completeHistory) {
            if (actualKpi.getStatDate().isBefore(testStart)) {
                continue;
            }
            List<DailyBusinessKpi> training = completeHistory.stream()
                    .filter(value -> value.getStatDate().isBefore(actualKpi.getStatDate()))
                    .filter(value -> !value.getStatDate().isBefore(actualKpi.getStatDate().minusDays(56)))
                    .toList();
            if (training.size() < 7) {
                continue;
            }
            BigDecimal predicted = prediction(
                    training, actualKpi.getStatDate(), extractor).value();
            BigDecimal actual = math.zero(extractor.apply(actualKpi));
            BigDecimal error = predicted.subtract(actual);
            errors.add(error);
            if (actual.signum() != 0) {
                percentageErrors.add(error.abs()
                        .divide(actual.abs(), 6, RoundingMode.HALF_UP));
            }
        }
        if (errors.isEmpty()) {
            return;
        }

        BigDecimal mae = averageValues(errors.stream().map(BigDecimal::abs).toList());
        double meanSquared = errors.stream()
                .mapToDouble(value -> Math.pow(value.doubleValue(), 2)).average().orElse(0);
        BigDecimal rmse = BigDecimal.valueOf(Math.sqrt(meanSquared));
        BigDecimal mape = percentageErrors.isEmpty()
                ? null : averageValues(percentageErrors);
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

    private record Prediction(BigDecimal value, BigDecimal interval, BigDecimal coverage) {
    }
}
