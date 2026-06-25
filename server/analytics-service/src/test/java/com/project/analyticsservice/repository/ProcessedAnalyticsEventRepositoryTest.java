package com.project.analyticsservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.analyticsservice.entity.ProcessedAnalyticsEvent;

@DataJpaTest
class ProcessedAnalyticsEventRepositoryTest {

    @Autowired
    private ProcessedAnalyticsEventRepository processedAnalyticsEventRepository;

    @Test
    void testSaveAndFindMethods() {
        ProcessedAnalyticsEvent event = new ProcessedAnalyticsEvent(
                null,
                "evt_123456",
                "PAYMENT_SUCCEEDED",
                "payment-service",
                null
        );

        ProcessedAnalyticsEvent saved = processedAnalyticsEventRepository.save(event);
        assertNotNull(saved.getId());
        assertNotNull(saved.getProcessedAt());

        Optional<ProcessedAnalyticsEvent> found = processedAnalyticsEventRepository.findByEventId("evt_123456");
        assertTrue(found.isPresent());
        assertEquals("PAYMENT_SUCCEEDED", found.get().getEventType());
        assertEquals("payment-service", found.get().getSourceService());

        assertTrue(processedAnalyticsEventRepository.existsByEventId("evt_123456"));
        assertFalse(processedAnalyticsEventRepository.existsByEventId("evt_non_existent"));
    }

    @Test
    void testUniqueEventIdConstraint() {
        ProcessedAnalyticsEvent event1 = new ProcessedAnalyticsEvent(
                null,
                "evt_duplicate",
                "BOOKING_CANCELLED",
                "booking-service",
                null
        );
        processedAnalyticsEventRepository.saveAndFlush(event1);

        ProcessedAnalyticsEvent event2 = new ProcessedAnalyticsEvent(
                null,
                "evt_duplicate",
                "PAYMENT_REFUNDED",
                "payment-service",
                null
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            processedAnalyticsEventRepository.saveAndFlush(event2);
        });
    }

    @Test
    void testFindAllByEventType() {
        processedAnalyticsEventRepository.save(new ProcessedAnalyticsEvent(null, "e1", "PAYMENT_SUCCEEDED", "payment-service", null));
        processedAnalyticsEventRepository.save(new ProcessedAnalyticsEvent(null, "e2", "PAYMENT_SUCCEEDED", "payment-service", null));
        processedAnalyticsEventRepository.save(new ProcessedAnalyticsEvent(null, "e3", "BOOKING_CANCELLED", "booking-service", null));
        processedAnalyticsEventRepository.flush();

        List<ProcessedAnalyticsEvent> succeededEvents = processedAnalyticsEventRepository
                .findAllByEventType("PAYMENT_SUCCEEDED");
        assertEquals(2, succeededEvents.size());
        assertTrue(succeededEvents.stream().allMatch(e -> "PAYMENT_SUCCEEDED".equals(e.getEventType())));

        List<ProcessedAnalyticsEvent> cancelledEvents = processedAnalyticsEventRepository
                .findAllByEventType("BOOKING_CANCELLED");
        assertEquals(1, cancelledEvents.size());
        assertEquals("e3", cancelledEvents.get(0).getEventId());
    }
}
