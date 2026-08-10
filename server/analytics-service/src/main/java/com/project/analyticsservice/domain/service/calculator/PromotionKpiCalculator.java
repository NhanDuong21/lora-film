package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.FactAnalysisService;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.domain.service.PerformanceAggregationService;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.entity.PromotionPerformanceDaily;
import com.project.analyticsservice.repository.PromotionPerformanceDailyRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Component
@Order(40)
public class PromotionKpiCalculator implements KpiCalculator {
    private final FactAnalysisService facts;
    private final PerformanceAggregationService aggregationService;
    private final PromotionPerformanceDailyRepository repository;
    private final MetricMathService math;

    public PromotionKpiCalculator(
            FactAnalysisService facts,
            PerformanceAggregationService aggregationService,
            PromotionPerformanceDailyRepository repository,
            MetricMathService math) {
        this.facts = facts;
        this.aggregationService = aggregationService;
        this.repository = repository;
        this.math = math;
    }

    @Override
    public String stage() {
        return "PROMOTION_KPI";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        FactAnalysisService.FactBundle bundle = facts.load(statDate);
        Map<String, PerformanceAggregationService.PerformanceAggregate> aggregates =
                aggregationService.aggregate(bundle, FactBookingMetric::getPromotionPublicId);
        aggregates.forEach((promotionKey, aggregate) -> {
            PromotionPerformanceDaily kpi =
                    repository.findByPromotionKeyAndStatDate(promotionKey, statDate)
                            .orElseGet(PromotionPerformanceDaily::new);
            kpi.setPromotionKey(promotionKey);
            kpi.setPromotionName(aggregate.bookings().stream()
                    .map(FactBookingMetric::getPromotionName)
                    .filter(name -> name != null && !name.isBlank())
                    .findFirst().orElse(promotionKey));
            kpi.setStatDate(statDate);
            kpi.setUsageCount(aggregate.bookingCount());
            kpi.setDiscountCost(math.money(aggregate.discountAmount()));
            kpi.setGeneratedRevenue(math.money(aggregate.netRevenue()));
            kpi.setRoi(math.divide(aggregate.netRevenue(), aggregate.discountAmount(), 6));
            repository.save(kpi);
        });
    }
}
