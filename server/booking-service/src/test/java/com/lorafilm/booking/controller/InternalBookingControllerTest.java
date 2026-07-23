package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.booking.controller.InternalBookingController;
import com.lorafilm.booking.booking.dto.BookingAdminResponse;
import com.lorafilm.booking.booking.dto.BookingPaymentContextDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultRequestDto;
import com.lorafilm.booking.booking.dto.BookingPaymentResultResponseDto;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.service.InternalBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
    public void getPaymentContext_Success_Returns200() throws Exception {
        BookingPaymentContextDto response = new BookingPaymentContextDto();
        response.setBookingId(1001L);
        response.setBookingStatus("PENDING_PAYMENT");
        response.setPayable(true);

        when(internalBookingService.getPaymentContext(1001L)).thenReturn(response);

        mockMvc.perform(get("/internal/bookings/1001/payment-context")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(1001))
                .andExpect(jsonPath("$.data.payable").value(true));
    }

    @Test
    public void processPaymentResult_Success_Returns200() throws Exception {
        BookingPaymentResultRequestDto request = new BookingPaymentResultRequestDto();
        request.setEventId("event-123");
        request.setResult("SUCCESS");

        BookingPaymentResultResponseDto response = new BookingPaymentResultResponseDto("event-123", true, false, "BOOKING_CONFIRMED");

        when(internalBookingService.processPaymentResult(eq(1001L), any(BookingPaymentResultRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/internal/bookings/1001/payment-results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value("event-123"))
                .andExpect(jsonPath("$.data.result").value("BOOKING_CONFIRMED"));
    }
}
