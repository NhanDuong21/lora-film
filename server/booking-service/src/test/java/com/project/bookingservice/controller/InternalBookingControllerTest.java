package com.project.bookingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bookingservice.dto.payment.PaymentContextResponse;
import com.project.bookingservice.dto.payment.PaymentResultRequest;
import com.project.bookingservice.dto.payment.PaymentResultResponse;
import com.project.bookingservice.service.InternalPaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.project.bookingservice.service.IdempotencyService;
import com.project.bookingservice.security.JwtProvider;

@WebMvcTest(controllers = InternalBookingController.class, properties = "internal.api.token=secret-token")
@AutoConfigureMockMvc(addFilters = false)
public class InternalBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InternalPaymentService internalPaymentService;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private JwtProvider jwtProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetPaymentContext_MissingToken_Returns401() throws Exception {
        mockMvc.perform(get("/internal/bookings/1/payment-context"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_TOKEN_INVALID"));
    }

    @Test
    public void testGetPaymentContext_InvalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/internal/bookings/1/payment-context")
                .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_TOKEN_INVALID"));
    }

    @Test
    public void testGetPaymentContext_ValidToken_Returns200() throws Exception {
        PaymentContextResponse response = new PaymentContextResponse();
        response.setBookingId(1L);
        Mockito.when(internalPaymentService.getPaymentContext(1L)).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/1/payment-context")
                .header("X-Internal-Token", "secret-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(1));
    }

    @Test
    public void testProcessPaymentResult_ValidToken_Returns200() throws Exception {
        PaymentResultRequest request = new PaymentResultRequest();
        request.setEventId(UUID.randomUUID().toString());
        request.setSchemaVersion("1.0");
        request.setPaymentId(100L);
        request.setPaymentTransactionCode("TX123");
        request.setPaymentMethod("VNPAY");
        request.setResult("SUCCESS");
        request.setAmount(new BigDecimal("100000.00"));
        request.setCurrency("VND");
        request.setOccurredAt(LocalDateTime.now());
        request.setReconciliationStatus("NONE");

        PaymentResultResponse response = new PaymentResultResponse();
        response.setEventId(request.getEventId());
        response.setApplied(true);
        response.setDuplicate(false);
        response.setResult("BOOKING_CONFIRMED");

        Mockito.when(internalPaymentService.processPaymentResult(eq(1L), any(PaymentResultRequest.class))).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/1/payment-results")
                .header("X-Internal-Token", "secret-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("BOOKING_CONFIRMED"));
    }
}
