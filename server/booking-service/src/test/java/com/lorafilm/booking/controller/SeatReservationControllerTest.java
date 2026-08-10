package com.lorafilm.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.infrastructure.entity.BookingIdempotencyKey;
import com.lorafilm.booking.infrastructure.enums.IdempotencyStatus;
import com.lorafilm.booking.infrastructure.service.IdempotencyService;
import com.lorafilm.booking.reservation.controller.SeatReservationController;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatResponse;
import com.lorafilm.booking.reservation.dto.ReleaseSeatRequest;
import com.lorafilm.booking.reservation.service.SeatReservationService;
import com.lorafilm.booking.security.jwt.JwtTokenProvider;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SeatReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeatReservationService seatReservationService;

    @MockBean
    private SecurityContextService securityContextService;

    @MockBean
    private IdempotencyService idempotencyService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.lorafilm.booking.common.filter.CorrelationIdFilter correlationIdFilter;

    @MockBean
    private com.lorafilm.booking.common.filter.RequestLoggingFilter requestLoggingFilter;

    @Test
    public void holdSeats_Success_Returns201() throws Exception {
        HoldSeatRequest request = new HoldSeatRequest(1001L, List.of(15L, 16L));
        HoldSeatResponse response = new HoldSeatResponse(List.of(101L, 102L), Instant.now().plusSeconds(300));

        when(securityContextService.getCurrentUserId()).thenReturn(100L);
        when(seatReservationService.holdSeats(eq(100L), any())).thenReturn(response);

        mockMvc.perform(post("/api/seat-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationIds[0]").value(101))
                .andExpect(jsonPath("$.reservationIds[1]").value(102))
                .andExpect(jsonPath("$.reservationPublicIds").isArray())
                .andExpect(jsonPath("$.reservationPublicIds[0]").exists());
    }

    @Test
    public void holdSeats_IdempotencyKeyCached_ReturnsCachedResponse() throws Exception {
        HoldSeatRequest request = new HoldSeatRequest(1001L, List.of(15L, 16L));
        HoldSeatResponse response = new HoldSeatResponse(List.of(101L, 102L), Instant.now().plusSeconds(300));
        String cachedResponseBody = objectMapper.writeValueAsString(response);

        BookingIdempotencyKey idempotencyKey = new BookingIdempotencyKey();
        idempotencyKey.setIdempotencyKey("test-key-123");
        idempotencyKey.setStatus(IdempotencyStatus.COMPLETED);
        idempotencyKey.setResponseStatus(201);
        idempotencyKey.setResponseBody(cachedResponseBody);

        when(securityContextService.getCurrentUserId()).thenReturn(100L);
        when(idempotencyService.checkKey("test-key-123")).thenReturn(Optional.of(idempotencyKey));

        mockMvc.perform(post("/api/seat-reservations")
                        .header("Idempotency-Key", "test-key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationIds[0]").value(101))
                .andExpect(jsonPath("$.reservationPublicIds").isArray());
    }

    @Test
    public void releaseSeats_Success_Returns200() throws Exception {
        ReleaseSeatRequest request = new ReleaseSeatRequest(List.of(101L), "User cancel");

        when(securityContextService.getCurrentUserId()).thenReturn(100L);

        mockMvc.perform(delete("/api/seat-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void getOccupiedSeatsByShowtime_Success_Returns200() throws Exception {
        com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse response = new com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse("1001", List.of());

        when(seatReservationService.getOccupiedSeatsByShowtime("1001")).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/seat-reservations/showtime/1001/occupied-seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.showtimeIdentifier").value("1001"));
    }

    @Test
    public void extendReservation_Success_Returns200() throws Exception {
        com.lorafilm.booking.reservation.dto.ExtendReservationResponse response = new com.lorafilm.booking.reservation.dto.ExtendReservationResponse(
                "pub-123", "RES-12345", Instant.now().plusSeconds(480), 180L);

        when(securityContextService.getCurrentUserId()).thenReturn(100L);
        when(seatReservationService.extendReservation("pub-123", 100L)).thenReturn(response);

        mockMvc.perform(post("/api/seat-reservations/pub-123/extend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value("pub-123"))
                .andExpect(jsonPath("$.extendedSeconds").value(180));
    }
}
