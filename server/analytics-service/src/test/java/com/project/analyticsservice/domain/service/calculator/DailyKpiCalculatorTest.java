package com.project.analyticsservice.domain.service.calculator;

import com.project.analyticsservice.domain.service.FactAnalysisService;
import com.project.analyticsservice.domain.service.MetricMathService;
import com.project.analyticsservice.entity.*;
import com.project.analyticsservice.repository.DailyBusinessKpiRepository;
import com.project.analyticsservice.repository.FactBookingCancellationRepository;
import com.project.analyticsservice.repository.FactBookingMetricRepository;
import com.project.analyticsservice.repository.FactPaymentRefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyKpiCalculatorTest {
    @Mock
    private FactBookingMetricRepository bookingRepository;
    @Mock
    private FactBookingCancellationRepository cancellationRepository;
    @Mock
    private FactPaymentRefundRepository refundRepository;
    @Mock
    private DailyBusinessKpiRepository repository;

    @Test
    void appliesCanonicalRevenueAndRateFormulas() {
        LocalDate date = LocalDate.of(2026, 7, 27);
        FactBookingMetric booking = new FactBookingMetric();
        booking.setBookingPublicId("booking-1");
        booking.setGrossAmount(new BigDecimal("120.00"));
        booking.setDiscountAmount(new BigDecimal("20.00"));
        booking.setNetRevenue(new BigDecimal("100.00"));
        booking.setTicketCount(2);
        booking.setPromotionPublicId("promo-1");
        booking.setBusinessDate(date);
        booking.setShowtimePublicId("showtime-1");
        booking.setAvailableSeats(4);

        FactPaymentRefund refund = new FactPaymentRefund();
        refund.setBookingPublicId("booking-1");
        refund.setRefundAmount(new BigDecimal("10.00"));
        FactBookingCancellation cancellation = new FactBookingCancellation();
        cancellation.setBookingKey("booking-2");

        when(bookingRepository.findAllByBusinessDate(date)).thenReturn(List.of(booking));
        when(bookingRepository.findAllByBusinessDateLessThanEqual(date)).thenReturn(List.of(booking));
        when(cancellationRepository.findAllByBusinessDate(date)).thenReturn(List.of(cancellation));
        when(refundRepository.findAllByRefundDate(date)).thenReturn(List.of(refund));
        when(refundRepository.findAllByRefundDateLessThanEqual(date)).thenReturn(List.of(refund));
        when(repository.findByStatDate(date)).thenReturn(Optional.empty());

        FactAnalysisService factService = new FactAnalysisService(
                bookingRepository, cancellationRepository, refundRepository, new MetricMathService());
        new DailyKpiCalculator(factService, repository, new MetricMathService()).calculate(date);

        ArgumentCaptor<DailyBusinessKpi> captor = ArgumentCaptor.forClass(DailyBusinessKpi.class);
        verify(repository).save(captor.capture());
        DailyBusinessKpi kpi = captor.getValue();
        assertEquals(new BigDecimal("90.00"), kpi.getNetRevenue());
        assertEquals(new BigDecimal("90.00"), kpi.getAverageBookingValue());
        assertEquals(new BigDecimal("1.000000"), kpi.getRefundRate());
        assertEquals(new BigDecimal("0.500000"), kpi.getCancelRate());
        assertEquals(new BigDecimal("1.000000"), kpi.getPromotionUsageRate());
    }
}
