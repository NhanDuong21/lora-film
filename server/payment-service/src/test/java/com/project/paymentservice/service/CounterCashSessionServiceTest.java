package com.project.paymentservice.service;

import com.project.paymentservice.dto.request.CloseCounterCashSessionRequest;
import com.project.paymentservice.dto.request.OpenCounterCashSessionRequest;
import com.project.paymentservice.entity.CounterCashSession;
import com.project.paymentservice.enumtype.CounterCashSessionStatus;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.CounterCashSessionRepository;
import com.project.paymentservice.repository.PaymentRefundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class CounterCashSessionServiceTest {
    @Mock
    private CounterCashSessionRepository sessionRepository;
    @Mock
    private CashPaymentDetailRepository cashRepository;
    @Mock
    private PaymentRefundRepository refundRepository;

    private CounterCashSessionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CounterCashSessionService(sessionRepository, cashRepository, refundRepository);
        when(sessionRepository.save(any(CounterCashSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cashRepository.sumSuccessfulCashCollected(any(), any(), any()))
                .thenReturn(new BigDecimal("350000.00"));
        when(cashRepository.countSuccessfulCashCollected(any(), any(), any())).thenReturn(3L);
        when(refundRepository.sumSuccessfulCashRefunds(any(), any(), any()))
                .thenReturn(new BigDecimal("50000.00"));
        when(refundRepository.countSuccessfulCashRefunds(any(), any(), any())).thenReturn(1L);
    }

    @Test
    void opensAndShowsLiveExpectedCash() {
        when(sessionRepository.findFirstByEmployeeAccountIdAndStatusOrderByOpenedAtDesc(
                63L, CounterCashSessionStatus.OPEN)).thenReturn(Optional.empty());

        var response = service.open(
                63L,
                "CINEMA-ONE",
                new OpenCounterCashSessionRequest(new BigDecimal("1000000"), "Nhận két từ ca sáng"));

        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.openingFloat()).isEqualByComparingTo("1000000.00");
        assertThat(response.cashSales()).isEqualByComparingTo("350000.00");
        assertThat(response.cashRefunds()).isEqualByComparingTo("50000.00");
        assertThat(response.expectedCash()).isEqualByComparingTo("1300000.00");
        assertThat(response.cashTransactionCount()).isEqualTo(3);
    }

    @Test
    void closesWithPersistedVariance() {
        CounterCashSession session = new CounterCashSession();
        session.setPublicId("4d235278-7c3d-4e67-a7f0-5ad1da07b654");
        session.setEmployeeAccountId(63L);
        session.setCinemaPublicId("cinema-one");
        session.setOpeningFloat(new BigDecimal("1000000"));
        session.setOpenedAt(Instant.parse("2026-08-10T01:00:00Z"));
        session.setStatus(CounterCashSessionStatus.OPEN);
        when(sessionRepository.findByPublicIdForUpdate(session.getPublicId()))
                .thenReturn(Optional.of(session));

        var response = service.close(
                63L,
                "CINEMA-ONE",
                session.getPublicId(),
                new CloseCounterCashSessionRequest(
                        new BigDecimal("1295000"),
                        "Bàn giao trực tiếp cho quản lý rạp"));

        assertThat(response.status()).isEqualTo("CLOSED");
        assertThat(response.expectedCash()).isEqualByComparingTo("1300000.00");
        assertThat(response.countedCash()).isEqualByComparingTo("1295000.00");
        assertThat(response.varianceAmount()).isEqualByComparingTo("-5000.00");
        assertThat(response.closedAt()).isNotNull();
    }
}
