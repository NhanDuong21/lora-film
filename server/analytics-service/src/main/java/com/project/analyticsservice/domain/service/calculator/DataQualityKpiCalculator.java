package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.entity.AnalyticsDataQualityDaily;
import com.project.analyticsservice.entity.DailyBusinessKpi;
import com.project.analyticsservice.repository.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
@Order(55)
public class DataQualityKpiCalculator implements KpiCalculator {
    private final DailyBusinessKpiRepository dailyRepository;
    private final FactBookingMetricRepository bookingRepository;
    private final FactBookingCancellationRepository cancellationRepository;
    private final FactPaymentRefundRepository refundRepository;
    private final AnalyticsDataQualityDailyRepository qualityRepository;

    public DataQualityKpiCalculator(
            DailyBusinessKpiRepository dailyRepository,
            FactBookingMetricRepository bookingRepository,
            FactBookingCancellationRepository cancellationRepository,
            FactPaymentRefundRepository refundRepository,
            AnalyticsDataQualityDailyRepository qualityRepository) {
        this.dailyRepository = dailyRepository;
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.refundRepository = refundRepository;
        this.qualityRepository = qualityRepository;
    }

    @Override
    public String stage() {
        return "DATA_QUALITY";
    }

    @Override
    @Transactional
    public void calculate(LocalDate statDate) {
        DailyBusinessKpi kpi = dailyRepository.findByStatDate(statDate).orElse(null);
        if (kpi == null) {
            return;
        }
        long bookingCount = bookingRepository.findAllByBusinessDate(statDate).size();
        long cancellationCount = cancellationRepository.findAllByBusinessDate(statDate).size();
        long refundCount = refundRepository.findAllByRefundDate(statDate).size();
        long accepted = bookingCount + cancellationCount + refundCount;

        AnalyticsDataQualityDaily quality = qualityRepository
                .findByStatDateAndSourceServiceAndEventType(
                        statDate, "ALL_SOURCES", "ALL_ANALYTICS_EVENTS")
                .orElseGet(AnalyticsDataQualityDaily::new);
        quality.setStatDate(statDate);
        quality.setSourceService("ALL_SOURCES");
        quality.setEventType("ALL_ANALYTICS_EVENTS");
        quality.setReceivedCount(accepted);
        quality.setAcceptedCount(accepted);
        quality.setDuplicateCount(0L);
        quality.setRejectedCount(0L);
        quality.setDlqCount(0L);
        quality.setLateEventCount(0L);
        quality.setAverageLagSeconds(BigDecimal.ZERO.setScale(6));
        quality.setMaximumLagSeconds(0L);
        quality.setCompletenessScore(kpi.getDataCompleteness());
        quality.setFreshnessStatus(accepted == 0
                ? "NO_DATA"
                : kpi.getDataCompleteness().compareTo(new BigDecimal("0.80")) >= 0
                    ? "FRESH" : "DEGRADED");
        quality.setCalculatedAt(Instant.now());
        qualityRepository.save(quality);
    }
}
