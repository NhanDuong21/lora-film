package com.lorafilm.booking.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPublishCustomerBookingOperationsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/bookings'].post").exists())
                .andExpect(jsonPath("$.paths['/api/bookings'].get").exists())
                .andExpect(jsonPath("$.paths['/api/bookings/{publicId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/bookings/{publicId}'].delete").exists())
                .andExpect(jsonPath("$.components.schemas.CreateBookingRequest.properties.showtimePublicId").exists())
                .andExpect(jsonPath("$.components.schemas.CreateBookingRequest.properties.showtimeId").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateBookingRequest.properties.reservationPublicIds").exists())
                .andExpect(jsonPath("$.components.schemas.CreateBookingRequest.properties.reservationIds").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CreateBookingRequest.properties.note").doesNotExist());
    }
}
