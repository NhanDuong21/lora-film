package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.controller.InternalBookingController;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.service.InternalBookingService;
import com.lorafilm.booking.booking.service.InternalBookingPaymentService;
import com.lorafilm.booking.booking.dto.response.InternalPaymentContextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class InternalBookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InternalBookingService internalBookingService;
    @Mock
    private InternalBookingPaymentService internalBookingPaymentService;

    @InjectMocks
    private InternalBookingController internalBookingController;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalBookingController).build();
    }

    @Test
    public void confirmBooking_Success_Returns200() throws Exception {
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        response.setBookingCode("BK1001");
        response.setBookingStatus(BookingStatus.CONFIRMED);

        when(internalBookingService.confirmBooking("550e8400-e29b-41d4-a716-446655440000")).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/550e8400-e29b-41d4-a716-446655440000/confirm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("CONFIRMED"));
    }

    @Test
    public void expireBooking_Success_Returns200() throws Exception {
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        response.setBookingCode("BK1001");
        response.setBookingStatus(BookingStatus.EXPIRED);

        when(internalBookingService.expireBooking("550e8400-e29b-41d4-a716-446655440000")).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/550e8400-e29b-41d4-a716-446655440000/expire")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("EXPIRED"));
    }

    @Test
    public void refundBooking_Success_Returns200() throws Exception {
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setPublicId("550e8400-e29b-41d4-a716-446655440000");
        response.setBookingCode("BK1001");
        response.setBookingStatus(BookingStatus.REFUNDED);

        when(internalBookingService.refundBooking("550e8400-e29b-41d4-a716-446655440000")).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/550e8400-e29b-41d4-a716-446655440000/refund")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("REFUNDED"));
    }

    @Test
    public void getBookingByCode_Success_Returns200() throws Exception {
        BookingAdminResponse response = new BookingAdminResponse();
        response.setId(10L);
        response.setBookingCode("BK1001");

        when(internalBookingService.getBookingByCode("BK1001")).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/code/BK1001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingCode").value("BK1001"));
    }

    @Test
    public void getPaymentContext_Success_ReturnsAuthoritativeAmount() throws Exception {
        InternalPaymentContextResponse response = new InternalPaymentContextResponse(
                10L,
                15L,
                "PENDING_PAYMENT",
                true,
                new BigDecimal("240000.00"),
                "VND",
                LocalDateTime.now().plusMinutes(15),
                new InternalPaymentContextResponse.AnalyticsSnapshot(101L, "Superman", 2));
        when(internalBookingPaymentService.getPaymentContext(10L)).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/10/payment-context")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount").value(240000.00))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.analyticsSnapshot.ticketCount").value(2));
    }
}
