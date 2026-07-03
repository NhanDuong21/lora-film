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
        Payment p = new Payment();
        p.setPaymentTransactionCode("TXN-QUERY-1");
        p.setBookingId(5L);
        p.setAccountId(20L);
        p.setAttemptNumber(1);
        p.setAmount(new BigDecimal("200000"));
        p.setPaymentMethod(PaymentMethod.VNPAY);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        paymentRepository.saveAndFlush(p);
        
        assertTrue(paymentRepository.findByPaymentTransactionCode("TXN-QUERY-1").isPresent());
        assertTrue(paymentRepository.findByBookingId(5L, PageRequest.of(0, 10)).getTotalElements() > 0);
    }
    
    @Test
    @Transactional
    void testOutboxSkipLocked() {
        PaymentOutboxEvent event = new PaymentOutboxEvent();
        event.setEventId("EVT-1");
        event.setAggregateType("PAYMENT");
        event.setAggregateId("1");
        event.setEventType("PAYMENT_SUCCESS");
        event.setSchemaVersion("1.0");
        event.setDestination(OutboxDestination.BOOKING_SERVICE_REST);
        event.setPayload("{}");
        event.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.saveAndFlush(event);
        
        List<PaymentOutboxEvent> claimed = outboxEventRepository.findAndClaimPendingEvents(LocalDateTime.now(), 10);
        assertFalse(claimed.isEmpty());
    }
}
