package com.lorafilm.booking.food.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.food.dto.request.AddFoodItemRequest;
import com.lorafilm.booking.food.dto.response.FoodOrderResponse;
import com.lorafilm.booking.food.service.BookingFoodItemService;
import com.lorafilm.booking.food.service.BookingFoodOrderService;
import com.lorafilm.booking.security.service.SecurityContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CustomerBookingFoodController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
class CustomerBookingFoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingFoodOrderService foodOrderService;

    @MockBean
    private BookingFoodItemService foodItemService;

    @MockBean
    private SecurityContextService securityContextService;

    @MockBean
    private com.lorafilm.booking.security.jwt.JwtTokenProvider jwtTokenProvider;

    private FoodOrderResponse dummyResponse;

    @BeforeEach
    void setUp() {
        dummyResponse = new FoodOrderResponse();
        dummyResponse.setPublicId("order-123");
        dummyResponse.setBookingId("booking-123");
        dummyResponse.setTotalQuantity(2);
        dummyResponse.setFinalAmount(new BigDecimal("100000"));
        dummyResponse.setItems(Collections.emptyList());
    }

    @Test
    void shouldGetFoodOrder() throws Exception {
        when(foodOrderService.getFoodOrder("booking-123")).thenReturn(dummyResponse);

        mockMvc.perform(get("/api/v1/bookings/booking-123/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Food order retrieved successfully"))
                .andExpect(jsonPath("$.data.publicId").value("order-123"));
    }

    @Test
    void shouldAddFoodItem() throws Exception {
        AddFoodItemRequest request = new AddFoodItemRequest();
        request.setProductId(1L);
        request.setQuantity(2);

        when(foodItemService.addFoodItem(eq("booking-123"), any(AddFoodItemRequest.class))).thenReturn(dummyResponse);

        mockMvc.perform(post("/api/v1/bookings/booking-123/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Food item added successfully"))
                .andExpect(jsonPath("$.data.publicId").value("order-123"));
    }
}
