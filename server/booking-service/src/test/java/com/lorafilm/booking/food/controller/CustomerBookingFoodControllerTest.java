package com.lorafilm.booking.food.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.FoodBookingFacadeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerBookingFoodController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerBookingFoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FoodBookingFacadeService facadeService;

    @MockBean
    private com.lorafilm.booking.security.service.SecurityContextService securityContextService;

    @MockBean
    private com.lorafilm.booking.security.jwt.JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAddFoodItemSuccessfully() throws Exception {
        String bookingId = "123-abc";
        AddFoodItemRequest request = new AddFoodItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(facadeService.addFoodItem(eq(bookingId), any())).thenReturn(new FoodOrderResponse());

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/foods", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
