package com.lorafilm.movie.showtime.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.showtime.dto.request.BookingContextRequest;
import com.lorafilm.movie.showtime.dto.response.BookingContextResponse;
import com.lorafilm.movie.showtime.dto.response.BookingContextShowtimeDto;
import com.lorafilm.movie.showtime.dto.response.BookingContextPricingDto;
import com.lorafilm.movie.showtime.service.ShowtimeBookingContextService;
import com.lorafilm.movie.showtime.service.ShowtimeQueryService;
import com.lorafilm.movie.common.security.InternalTokenFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.TestPropertySource;

@WebMvcTest(
    value = InternalShowtimeController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = com.lorafilm.movie.common.security.JwtFilter.class
    )
)
@Import({InternalTokenFilter.class})
@TestPropertySource(properties = "app.internal-token=secret-internal-token")
class InternalShowtimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShowtimeBookingContextService showtimeBookingContextService;

    @MockBean
    private ShowtimeQueryService showtimeQueryService;


    // Since InternalTokenFilter reads this value from property, we might need to mock or set it.
    // However, @WebMvcTest usually loads properties from application.properties if available.
    // Let's assume the default "secret-internal-token" is used if not set.
    private final String validToken = "secret-internal-token";

    @Test
    void getBookingContext_valid_returnsOk() throws Exception {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Arrays.asList(1L, 2L));

        BookingContextResponse response = new BookingContextResponse();
        BookingContextShowtimeDto showtimeDto = new BookingContextShowtimeDto();
        showtimeDto.setId(10L);
        response.setShowtime(showtimeDto);
        response.setMovieId(1L);
        response.setCinemaId(2L);
        response.setAuditoriumId(3L);

        BookingContextPricingDto pricingDto = new BookingContextPricingDto();
        pricingDto.setTotalAmount(new java.math.BigDecimal("200000"));
        response.setPricing(pricingDto);

        when(showtimeBookingContextService.getBookingContext(eq(10L), any(BookingContextRequest.class))).thenReturn(response);

        mockMvc.perform(post("/internal/showtimes/10/booking-context")
                .header("X-Internal-Token", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.showtime.id").value(10))
                .andExpect(jsonPath("$.data.movieId").value(1))
                .andExpect(jsonPath("$.data.cinemaId").value(2))
                .andExpect(jsonPath("$.data.auditoriumId").value(3))
                .andExpect(jsonPath("$.data.pricing.totalAmount").value(200000));
    }

    @Test
    void getBookingContext_missingToken_returnsUnauthorized() throws Exception {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Arrays.asList(1L, 2L));

        mockMvc.perform(post("/internal/showtimes/10/booking-context")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_401_UNAUTHORIZED"));
    }

    @Test
    void getBookingContext_invalidToken_returnsForbidden() throws Exception {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Arrays.asList(1L, 2L));

        mockMvc.perform(post("/internal/showtimes/10/booking-context")
                .header("X-Internal-Token", "wrong-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_403_FORBIDDEN"));
    }

    @Test
    void getBookingContext_emptySeatList_returnsBadRequest() throws Exception {
        BookingContextRequest request = new BookingContextRequest();
        request.setSeatIds(Collections.emptyList());

        mockMvc.perform(post("/internal/showtimes/10/booking-context")
                .header("X-Internal-Token", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    void getSeatLayout_valid_returnsOk() throws Exception {
        com.lorafilm.movie.showtime.dto.SeatLayoutDto layoutDto = new com.lorafilm.movie.showtime.dto.SeatLayoutDto();
        layoutDto.setShowtimeId(10L);
        layoutDto.setShowtimePublicId("showtime-pub-id");

        when(showtimeQueryService.getSeatLayout(eq(10L))).thenReturn(layoutDto);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/internal/showtimes/10/seat-layout")
                .header("X-Internal-Token", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.showtimeId").value(10))
                .andExpect(jsonPath("$.data.showtimePublicId").value("showtime-pub-id"));
    }
}
