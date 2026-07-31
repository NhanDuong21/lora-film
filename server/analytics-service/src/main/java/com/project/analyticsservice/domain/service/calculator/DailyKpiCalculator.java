package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.FactAnalysisService;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(10)
public class DailyKpiCalculator implements KpiCalculator {
    private final FactAnalysisService facts;
    private final DailyBusinessKpiRepository repository;
    private final MetricMathService math;

    public DailyKpiCalculator(
            FactAnalysisService facts,
            DailyBusinessKpiRepository repository,
            MetricMathService math) {
        this.facts = facts;
        this.repository = repository;
        this.math = math;
    }

    @Override
    public String stage() {
        return "DAILY_KPI";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        FactAnalysisService.FactBundle bundle = facts.load(statDate);
        BigDecimal gross = math.sum(bundle.bookings().stream().map(FactBookingMetric::getGrossAmount).toList());
        BigDecimal discount = math.sum(bundle.bookings().stream().map(FactBookingMetric::getDiscountAmount).toList());
        BigDecimal refunds = math.sum(bundle.refunds().stream().map(refund -> refund.getRefundAmount()).toList());
        BigDecimal netRevenue = gross.subtract(discount).subtract(refunds);
        long bookingCount = bundle.bookings().stream()
                .map(FactBookingMetric::getBookingPublicId).distinct().count();
        long refundBookingCount = bundle.refunds().stream()
                .map(refund -> refund.getBookingPublicId()).distinct().count();
        long cancellationCount = bundle.cancellations().stream()
                .map(cancel -> cancel.getBookingKey()).distinct().count();
        long ticketCount = bundle.bookings().stream()
                .mapToLong(FactBookingMetric::getTicketCount).sum();
        long promotionCount = bundle.bookings().stream()
                .filter(fact -> fact.getPromotionPublicId() != null).count();

        Map<String, LocalDate> firstBookingDate = bundle.historicalBookings().stream()
                .filter(fact -> fact.getUserPublicId() != null)
                .collect(Collectors.toMap(
                        FactBookingMetric::getUserPublicId,
                        FactBookingMetric::getBusinessDate,
                        (first, second) -> first.isBefore(second) ? first : second));
        Map<String, FactBookingMetric> currentUsers = bundle.bookings().stream()
                .filter(fact -> fact.getUserPublicId() != null)
                .collect(Collectors.toMap(
                        FactBookingMetric::getUserPublicId,
                        Function.identity(),
                        (first, ignored) -> first));
        long newCustomers = currentUsers.keySet().stream()
                .filter(user -> Objects.equals(firstBookingDate.get(user), statDate)).count();
        long returningCustomers = currentUsers.size() - newCustomers;

        DailyBusinessKpi kpi = repository.findByStatDate(statDate).orElseGet(DailyBusinessKpi::new);
        kpi.setStatDate(statDate);
        kpi.setGrossRevenue(math.money(gross));
        kpi.setDiscountAmount(math.money(discount));
        kpi.setRefundAmount(math.money(refunds));
        kpi.setNetRevenue(math.money(netRevenue));
        kpi.setBookingCount(bookingCount);
        kpi.setRefundBookingCount(refundBookingCount);
        kpi.setCancelledBookingCount(cancellationCount);
        kpi.setTicketCount(ticketCount);
        kpi.setNewCustomerCount(newCustomers);
        kpi.setReturningCustomerCount(returningCustomers);
        kpi.setAverageBookingValue(math.money(math.ratio(netRevenue, bookingCount)));
        kpi.setAverageTicketPrice(math.money(math.ratio(netRevenue, ticketCount)));
        kpi.setRefundRate(math.ratio(refundBookingCount, bookingCount));
        kpi.setCancelRate(math.ratio(cancellationCount, bookingCount + cancellationCount));
        kpi.setPromotionUsageRate(math.ratio(promotionCount, bookingCount));
        kpi.setOccupancyRate(facts.occupancyRate(bundle.bookings()));
        kpi.setDataCompleteness(facts.completeness(bundle.bookings()));
        repository.save(kpi);
    }
}
