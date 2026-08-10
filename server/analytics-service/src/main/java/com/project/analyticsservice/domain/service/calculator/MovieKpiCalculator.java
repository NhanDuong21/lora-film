package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.FactAnalysisService;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.domain.service.PerformanceAggregationService;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.entity.MoviePerformanceDaily;
import com.project.analyticsservice.repository.MoviePerformanceDailyRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Component
@Order(30)
public class MovieKpiCalculator implements KpiCalculator {
    private final FactAnalysisService facts;
    private final PerformanceAggregationService aggregationService;
    private final MoviePerformanceDailyRepository repository;
    private final MetricMathService math;

    public MovieKpiCalculator(
            FactAnalysisService facts,
            PerformanceAggregationService aggregationService,
            MoviePerformanceDailyRepository repository,
            MetricMathService math) {
        this.facts = facts;
        this.aggregationService = aggregationService;
        this.repository = repository;
        this.math = math;
    }

    @Override
    public String stage() {
        return "MOVIE_KPI";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        FactAnalysisService.FactBundle bundle = facts.load(statDate);
        Map<String, PerformanceAggregationService.PerformanceAggregate> aggregates =
                aggregationService.aggregate(bundle, FactBookingMetric::getMovieKey);
        aggregates.forEach((movieKey, aggregate) -> {
            FactBookingMetric snapshot = aggregate.bookings().stream().findFirst()
                    .orElseGet(() -> bundle.historicalBookings().stream()
                            .filter(fact -> movieKey.equals(fact.getMovieKey()))
                            .findFirst().orElse(null));
            if (snapshot == null) {
                return;
            }
            MoviePerformanceDaily kpi = repository.findByMovieKeyAndStatDate(movieKey, statDate)
                    .orElseGet(MoviePerformanceDaily::new);
            kpi.setMovieKey(movieKey);
            kpi.setMovieId(snapshot.getMovieId());
            kpi.setMovieTitle(snapshot.getMovieTitle());
            kpi.setStatDate(statDate);
            kpi.setGrossRevenue(math.money(aggregate.grossRevenue()));
            kpi.setDiscountAmount(math.money(aggregate.discountAmount()));
            kpi.setRefundAmount(math.money(aggregate.refundAmount()));
            kpi.setNetRevenue(math.money(aggregate.netRevenue()));
            kpi.setBookingCount(aggregate.bookingCount());
            kpi.setTicketCount(aggregate.ticketCount());
            kpi.setOccupancyRate(facts.occupancyRate(aggregate.bookings()));
            kpi.setRefundRate(math.ratio(
                    aggregate.refundBookingCount(), aggregate.bookingCount()));
            repository.save(kpi);
        });
    }
}
