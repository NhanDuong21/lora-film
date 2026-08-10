package com.lorafilm.booking.common;

import com.lorafilm.booking.common.exception.GlobalExceptionHandler;
import com.lorafilm.booking.common.exception.PaymentResultConflictException;
import com.lorafilm.booking.common.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void paymentConflictExposesPersistedReconciliationReference() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PaymentResultConflictException exception = new PaymentResultConflictException(
                "LATE_PAYMENT_SUCCESS",
                "Payment success arrived after the Booking deadline",
                "550e8400-e29b-41d4-a716-446655440000");

        ResponseEntity<ErrorResponse> response =
                handler.handlePaymentResultConflict(exception);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("LATE_PAYMENT_SUCCESS", response.getBody().getErrorCode());
        assertEquals(
                "550e8400-e29b-41d4-a716-446655440000",
                response.getBody().getReconciliationTaskPublicId());
    }
}
