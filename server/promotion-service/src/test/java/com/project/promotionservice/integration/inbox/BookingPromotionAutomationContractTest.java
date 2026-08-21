package com.project.promotionservice.integration.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.enums.AutomationRunStatus;
import com.project.promotionservice.automation.service.PromotionAutomationService;
import com.project.promotionservice.reservation.service.PromotionReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingPromotionAutomationContractTest {
    @Mock PromotionReservationService reservations;
    @Mock CacheManager cacheManager;
    @Mock PromotionAutomationService automation;
    private IntegrationEventProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new IntegrationEventProcessor(
                new ObjectMapper(), reservations, cacheManager);
        processor.setAutomationService(automation);
    }

    @Test
    void firstEligibleConfirmedBookingCreatesOneStableAutomationRunAndJob() {
        PromotionAutomationRun run = new PromotionAutomationRun();
        run.setPublicId("run-1");
        run.setStatus(AutomationRunStatus.AUDIENCE_READY);
        when(automation.createSecondBookingRun("42", "booking-1"))
                .thenReturn(run);

        boolean ignored = processor.process(event("confirmed-1", "BOOKING_CONFIRMED", """
                {"data":{"publicId":"booking-1","automationCustomerId":"42",
                 "firstConfirmedBooking":true,"ticketIssued":true,
                 "automationEligible":true}}
                """));

        assertThat(ignored).isFalse();
        verify(automation).createSecondBookingRun("42", "booking-1");
        verify(automation).ensureIssueJob("run-1", 200);
    }

    @Test
    void duplicateConfirmationUsesIdempotentJobSchedulingContract() {
        PromotionAutomationRun run = new PromotionAutomationRun();
        run.setPublicId("stable-run");
        run.setStatus(AutomationRunStatus.AUDIENCE_READY);
        when(automation.createSecondBookingRun("42", "booking-1"))
                .thenReturn(run);
        PromotionIntegrationEvent first = event("confirmed-1", "BOOKING_CONFIRMED", payload());
        PromotionIntegrationEvent replay = event("confirmed-replay", "BOOKING_CONFIRMED", payload());

        processor.process(first);
        processor.process(replay);

        verify(automation, times(2)).createSecondBookingRun("42", "booking-1");
        verify(automation, times(2)).ensureIssueJob("stable-run", 200);
    }

    @Test
    void refundIsForwardedEvenWhenItArrivesBeforeConfirmation() {
        processor.process(event("refund-1", "BOOKING_REFUNDED",
                "{\"data\":{\"bookingPublicId\":\"booking-1\"}}"));

        verify(automation).revokeSecondBookingForRefund("booking-1");
    }

    @Test
    void nonEligibleBookingCannotIssueAnEntitlement() {
        processor.process(event("confirmed-1", "BOOKING_CONFIRMED", """
                {"data":{"publicId":"booking-1","automationCustomerId":"42",
                 "firstConfirmedBooking":true,"ticketIssued":false,
                 "automationEligible":true}}
                """));

        verifyNoInteractions(automation);
    }

    private String payload() {
        return """
                {"data":{"publicId":"booking-1","automationCustomerId":"42",
                 "firstConfirmedBooking":true,"ticketIssued":true,
                 "automationEligible":true}}
                """;
    }

    private PromotionIntegrationEvent event(String id, String type, String payload) {
        PromotionIntegrationEvent event = new PromotionIntegrationEvent();
        event.setEventId(id);
        event.setEventType(type);
        event.setPayload(payload);
        return event;
    }
}
