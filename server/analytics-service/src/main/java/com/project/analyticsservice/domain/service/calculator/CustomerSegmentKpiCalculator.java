package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.FactAnalysisService;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.CustomerSegmentDaily;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.repository.CustomerSegmentDailyRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(50)
public class CustomerSegmentKpiCalculator implements KpiCalculator {
    private final FactAnalysisService facts;
    private final CustomerSegmentDailyRepository repository;
    private final MetricMathService math;

    public CustomerSegmentKpiCalculator(
            FactAnalysisService facts,
            CustomerSegmentDailyRepository repository,
            MetricMathService math) {
        this.facts = facts;
        this.repository = repository;
        this.math = math;
    }

    @Override
    public String stage() {
        return "CUSTOMER_SEGMENT";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        FactAnalysisService.FactBundle bundle = facts.load(statDate);
        Map<String, LocalDate> firstBookingDate = bundle.historicalBookings().stream()
                .filter(fact -> fact.getUserPublicId() != null)
                .collect(Collectors.toMap(
                        FactBookingMetric::getUserPublicId,
                        FactBookingMetric::getBusinessDate,
                        (first, second) -> first.isBefore(second) ? first : second));
        Map<String, List<FactBookingMetric>> historicalByTier = bundle.historicalBookings().stream()
                .filter(fact -> fact.getUserPublicId() != null)
                .collect(Collectors.groupingBy(FactBookingMetric::getMembershipTier));
        Map<String, List<FactBookingMetric>> currentByTier = bundle.bookings().stream()
                .filter(fact -> fact.getUserPublicId() != null)
                .collect(Collectors.groupingBy(FactBookingMetric::getMembershipTier));

        Set<String> tiers = new TreeSet<>(historicalByTier.keySet());
        tiers.addAll(currentByTier.keySet());
        for (String tier : tiers) {
            List<FactBookingMetric> historical = historicalByTier.getOrDefault(tier, List.of());
            List<FactBookingMetric> current = currentByTier.getOrDefault(tier, List.of());
            Set<String> customers = historical.stream()
                    .map(FactBookingMetric::getUserPublicId).collect(Collectors.toSet());
            Set<String> active = current.stream()
                    .map(FactBookingMetric::getUserPublicId).collect(Collectors.toSet());
            long newUsers = active.stream()
                    .filter(user -> Objects.equals(firstBookingDate.get(user), statDate)).count();
            BigDecimal lifetimeSpending =
                    math.sum(historical.stream().map(FactBookingMetric::getNetRevenue).toList());
            BigDecimal dailySpending =
                    math.sum(current.stream().map(FactBookingMetric::getNetRevenue).toList());
            BigDecimal lifetimeRefunds = math.sum(bundle.historicalRefunds().stream()
                    .filter(refund -> {
                        FactBookingMetric booking = bundle.bookingByKey().get(refund.getBookingPublicId());
                        return booking != null && tier.equals(booking.getMembershipTier());
                    })
                    .map(refund -> refund.getRefundAmount()).toList());
            BigDecimal dailyRefunds = math.sum(bundle.refunds().stream()
                    .filter(refund -> {
                        FactBookingMetric booking = bundle.bookingByKey().get(refund.getBookingPublicId());
                        return booking != null && tier.equals(booking.getMembershipTier());
                    })
                    .map(refund -> refund.getRefundAmount()).toList());
            lifetimeSpending = lifetimeSpending.subtract(lifetimeRefunds);
            dailySpending = dailySpending.subtract(dailyRefunds);

            CustomerSegmentDaily kpi = repository.findByMembershipTierAndStatDate(tier, statDate)
                    .orElseGet(CustomerSegmentDaily::new);
            kpi.setStatDate(statDate);
            kpi.setMembershipTier(tier);
            kpi.setActiveUsers((long) active.size());
            kpi.setNewUsers(newUsers);
            kpi.setReturningUsers(active.size() - newUsers);
            kpi.setTotalSpending(math.money(lifetimeSpending));
            kpi.setAverageSpending(math.money(math.ratio(dailySpending, active.size())));
            kpi.setCustomerLifetimeValue(math.money(math.ratio(lifetimeSpending, customers.size())));
            repository.save(kpi);
        }
    }
}
