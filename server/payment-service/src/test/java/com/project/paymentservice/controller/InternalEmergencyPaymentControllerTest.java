package com.project.paymentservice.controller;

import com.project.paymentservice.dto.request.EmergencyPaymentStopRequest;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.service.PaymentTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalEmergencyPaymentControllerTest {

    private PaymentTransactionService service;
    private InternalEmergencyPaymentController controller;
    private EmergencyPaymentStopRequest request;

    @BeforeEach
    void setUp() {
        service = mock(PaymentTransactionService.class);
        controller = new InternalEmergencyPaymentController(
                service, "trigger-token", "promotion-assessment-token");
        request = new EmergencyPaymentStopRequest(
                List.of("booking-1"), "Promotion incident assessment");
    }

    @Test
    void promotionTokenCanAssessWithoutReceivingStopAuthority() {
        when(service.assessPaymentsForEmergency(request.bookingPublicIds()))
                .thenReturn(new PaymentTransactionService.EmergencyPaymentAssessmentResult(
                        List.of(), List.of()));

        var response = controller.assessPayments(
                "promotion-assessment-token", request);

        assertEquals(200, response.getStatusCode().value());
        verify(service).assessPaymentsForEmergency(request.bookingPublicIds());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.stopPendingPayments(
                        "promotion-assessment-token", request));
        assertEquals("INTERNAL_TOKEN_INVALID", exception.getErrorCode());
        verify(service, never()).stopActiveAttemptsForEmergency(
                request.bookingPublicIds(), request.reason());
    }

    @Test
    void missingPromotionAssessmentTokenFailsStartupValidation() {
        assertThrows(IllegalStateException.class,
                () -> new InternalEmergencyPaymentController(
                        service, "trigger-token", ""));
    }
}
