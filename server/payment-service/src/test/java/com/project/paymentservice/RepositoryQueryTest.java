package com.project.paymentservice;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.enumtype.OutboxDestination;
import com.project.paymentservice.enumtype.OutboxStatus;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import com.project.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class RepositoryQueryTest {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentOutboxEventRepository outboxEventRepository;

    @Test
    @Transactional
    void testQueries() {
        Payment p = Payment.builder()
                .paymentTransactionCode("TXN-QUERY-1")
                .bookingId(5L)
                .accountId(20L)
                .attemptNumber(1)
                .amount(new BigDecimal("200000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p);
        
        assertTrue(paymentRepository.findByPaymentTransactionCode("TXN-QUERY-1").isPresent());
        assertTrue(paymentRepository.findByBookingId(5L, PageRequest.of(0, 10)).getTotalElements() > 0);
    }
    
    @Test
    @Transactional
    void testOutboxSkipLocked() {
        PaymentOutboxEvent event = PaymentOutboxEvent.builder()
                .eventId("EVT-1")
                .aggregateType("PAYMENT")
                .aggregateId("1")
                .eventType("PAYMENT_SUCCESS")
                .schemaVersion("1.0")
                .destination(OutboxDestination.BOOKING_SERVICE_REST)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .build();
        outboxEventRepository.saveAndFlush(event);
        
        List<PaymentOutboxEvent> claimed = outboxEventRepository.findAndClaimPendingEvents(LocalDateTime.now(), 10);
        assertFalse(claimed.isEmpty());
    }
}
