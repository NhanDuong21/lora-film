package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.reservation.controller.InternalSeatReservationController;
import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.dto.SeatAvailabilityResponse;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalSeatReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class InternalSeatReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeatReservationService seatReservationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.lorafilm.booking.common.filter.CorrelationIdFilter correlationIdFilter;

    @MockBean
    private com.lorafilm.booking.common.filter.RequestLoggingFilter requestLoggingFilter;

    @Test
    public void convertReservation_Success_Returns200() throws Exception {
        ConvertReservationRequest request = new ConvertReservationRequest(50L, List.of(101L));

        doNothing().when(seatReservationService).convertReservations(any());

        mockMvc.perform(post("/internal/seat-reservations/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void checkAvailability_Success_ReturnsAvailability() throws Exception {
        SeatAvailabilityResponse response = new SeatAvailabilityResponse(true, List.of());

        when(seatReservationService.checkAvailability(eq(1001L), anyList())).thenReturn(response);

        mockMvc.perform(get("/internal/seat-reservations/availability")
                        .param("showtimeId", "1001")
                        .param("seatIds", "15,16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }
}
