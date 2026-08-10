package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.FactAnalysisService;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.domain.service.PerformanceAggregationService;
import com.project.analyticsservice.entity.CinemaPerformanceDaily;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.repository.CinemaPerformanceDailyRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Component
@Order(20)
public class CinemaKpiCalculator implements KpiCalculator {
    private final FactAnalysisService facts;
    private final PerformanceAggregationService aggregationService;
    private final CinemaPerformanceDailyRepository repository;
    private final MetricMathService math;

    public CinemaKpiCalculator(
            FactAnalysisService facts,
            PerformanceAggregationService aggregationService,
            CinemaPerformanceDailyRepository repository,
            MetricMathService math) {
        this.facts = facts;
        this.aggregationService = aggregationService;
        this.repository = repository;
        this.math = math;
    }

    @Override
    public String stage() {
        return "CINEMA_KPI";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        FactAnalysisService.FactBundle bundle = facts.load(statDate);
        Map<String, PerformanceAggregationService.PerformanceAggregate> aggregates =
                aggregationService.aggregate(bundle, FactBookingMetric::getCinemaPublicId);
        aggregates.forEach((cinemaKey, aggregate) -> {
            CinemaPerformanceDaily kpi = repository.findByCinemaKeyAndStatDate(cinemaKey, statDate)
                    .orElseGet(CinemaPerformanceDaily::new);
            kpi.setCinemaKey(cinemaKey);
            kpi.setCinemaName(aggregate.bookings().stream()
                    .map(FactBookingMetric::getCinemaName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst().orElse(cinemaKey));
            kpi.setStatDate(statDate);
            kpi.setGrossRevenue(math.money(aggregate.grossRevenue()));
            kpi.setDiscountAmount(math.money(aggregate.discountAmount()));
            kpi.setRefundAmount(math.money(aggregate.refundAmount()));
            kpi.setNetRevenue(math.money(aggregate.netRevenue()));
            kpi.setBookingCount(aggregate.bookingCount());
            kpi.setTicketCount(aggregate.ticketCount());
            kpi.setOccupancyRate(facts.occupancyRate(aggregate.bookings()));
            kpi.setAverageBookingValue(math.money(
                    math.ratio(aggregate.netRevenue(), aggregate.bookingCount())));
            kpi.setRefundRate(math.ratio(
                    aggregate.refundBookingCount(), aggregate.bookingCount()));
            repository.save(kpi);
        });
    }
}
