package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.entity.FactPaymentRefund;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

@Service
public class PerformanceAggregationService {
    private final MetricMathService math;

    public PerformanceAggregationService(MetricMathService math) {
        this.math = math;
    }

    public Map<String, PerformanceAggregate> aggregate(
            FactAnalysisService.FactBundle bundle,
            Function<FactBookingMetric, String> keyExtractor) {
        Map<String, List<FactBookingMetric>> bookingsByKey = new LinkedHashMap<>();
        for (FactBookingMetric fact : bundle.bookings()) {
            String key = keyExtractor.apply(fact);
            if (StringUtils.hasText(key)) {
                bookingsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(fact);
            }
        }

        Map<String, List<FactPaymentRefund>> refundsByKey = new LinkedHashMap<>();
        for (FactPaymentRefund refund : bundle.refunds()) {
            FactBookingMetric original = bundle.bookingByKey().get(refund.getBookingPublicId());
            if (original == null) {
                continue;
            }
            String key = keyExtractor.apply(original);
            if (StringUtils.hasText(key)) {
                refundsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(refund);
            }
        }

        Set<String> keys = new LinkedHashSet<>(bookingsByKey.keySet());
        keys.addAll(refundsByKey.keySet());
        Map<String, PerformanceAggregate> result = new LinkedHashMap<>();
        for (String key : keys) {
            List<FactBookingMetric> bookings = bookingsByKey.getOrDefault(key, List.of());
            List<FactPaymentRefund> refunds = refundsByKey.getOrDefault(key, List.of());
            BigDecimal gross = math.sum(bookings.stream().map(FactBookingMetric::getGrossAmount).toList());
            BigDecimal discount = math.sum(bookings.stream().map(FactBookingMetric::getDiscountAmount).toList());
            BigDecimal refundAmount = math.sum(refunds.stream().map(FactPaymentRefund::getRefundAmount).toList());
            long bookingCount = bookings.stream()
                    .map(FactBookingMetric::getBookingPublicId).distinct().count();
            long ticketCount = bookings.stream().mapToLong(FactBookingMetric::getTicketCount).sum();
            long refundCount = refunds.stream()
                    .map(FactPaymentRefund::getBookingPublicId).distinct().count();
            result.put(key, new PerformanceAggregate(
                    bookings,
                    gross,
                    discount,
                    refundAmount,
                    gross.subtract(discount).subtract(refundAmount),
                    bookingCount,
                    ticketCount,
                    refundCount));
        }
        return result;
    }

    public record PerformanceAggregate(
            List<FactBookingMetric> bookings,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal netRevenue,
            long bookingCount,
            long ticketCount,
            long refundBookingCount) {
    }
}
