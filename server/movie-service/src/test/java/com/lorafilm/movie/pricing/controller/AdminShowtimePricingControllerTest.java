package com.lorafilm.movie.pricing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.pricing.dto.request.ShowtimePriceItemRequest;
import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.ShowtimePriceDto;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminShowtimePricingController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple controller testing
public class AdminShowtimePricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ShowtimePricingService showtimePricingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetPrices() throws Exception {
        ShowtimePricesResponse response = new ShowtimePricesResponse("VND",
                Collections.singletonList(new ShowtimePriceDto("vip-id", new BigDecimal("100000"))));

        when(showtimePricingService.getPrices("showtime-id")).thenReturn(response);

        mockMvc.perform(get("/api/admin/showtimes/showtime-id/prices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.prices[0].seatTypeId").value("vip-id"))
                .andExpect(jsonPath("$.data.prices[0].price").value(100000));
    }

    @Test
    void testUpdatePricesSuccess() throws Exception {
        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        ShowtimePriceItemRequest item = new ShowtimePriceItemRequest();
        item.setSeatTypeId("vip-id");
        item.setPrice(new BigDecimal("120000"));
        request.setPrices(Collections.singletonList(item));

        ShowtimePricesResponse response = new ShowtimePricesResponse("VND",
                Collections.singletonList(new ShowtimePriceDto("vip-id", new BigDecimal("120000"))));

        when(showtimePricingService.updatePrices(eq("showtime-id"), any(UpdateShowtimePricesRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/showtimes/showtime-id/prices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.prices[0].price").value(120000));
    }

    @Test
    void testNegativePriceValidationFails() throws Exception {
        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        ShowtimePriceItemRequest item = new ShowtimePriceItemRequest();
        item.setSeatTypeId("vip-id");
        item.setPrice(new BigDecimal("-100")); // Invalid negative price
        request.setPrices(Collections.singletonList(item));

        mockMvc.perform(put("/api/admin/showtimes/showtime-id/prices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEmptyPriceListValidationFails() throws Exception {
        UpdateShowtimePricesRequest request = new UpdateShowtimePricesRequest();
        request.setPrices(Collections.emptyList()); // Empty prices might fail depending on NotEmpty

        mockMvc.perform(put("/api/admin/showtimes/showtime-id/prices")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
