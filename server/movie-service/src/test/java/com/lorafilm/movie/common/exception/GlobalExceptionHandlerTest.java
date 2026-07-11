package com.lorafilm.movie.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.seat.controller.AdminSeatTypeController;
import com.lorafilm.movie.seat.controller.AdminSeatController;
import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import com.lorafilm.movie.seat.dto.BulkSeatItemRequest;
import com.lorafilm.movie.seat.dto.CreateSeatTypeRequest;
import com.lorafilm.movie.seat.domain.enums.SeatStatus;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.seat.service.SeatTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminSeatTypeController.class, AdminSeatController.class})
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SeatTypeService seatTypeService;

    @MockBean
    private SeatService seatService;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void testBeanValidation() throws Exception {
        CreateSeatTypeRequest request = new CreateSeatTypeRequest(null, "", "");

        mockMvc.perform(post("/api/admin/seat-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.fieldErrors").isArray())
                .andExpect(jsonPath("$.data.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.data.fieldErrors[0].field").value("code"))
                .andExpect(jsonPath("$.data.fieldErrors[1].field").value("name"));
    }

    @Test
    void testInvalidEnumRoot() throws Exception {
        String invalidJson = "{ \"code\": \"INVALID_ENUM\", \"name\": \"Standard\" }";

        mockMvc.perform(post("/api/admin/seat-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.data.field").value("code"));
    }

    @Test
    void testInvalidEnumBulk() throws Exception {
        String invalidJson = "{ \"seats\": [ { \"seatTypePublicId\": \"id\", \"rowLabel\": \"A\", \"seatNumber\": 1, \"seatCode\": \"A1\", \"positionRow\": 1, \"positionColumn\": 1, \"status\": \"ACTIVE\" }, { \"seatTypePublicId\": \"id\", \"rowLabel\": \"A\", \"seatNumber\": 2, \"seatCode\": \"A2\", \"positionRow\": 1, \"positionColumn\": 2, \"status\": \"INVALID_ENUM\" } ] }";

        mockMvc.perform(post("/api/admin/auditoriums/some-id/seats/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ENUM_VALUE"))
                .andExpect(jsonPath("$.data.field").value("seats[1].status"));
    }

    @Test
    void testMalformedJson() throws Exception {
        String invalidJson = "{ \"code\": ";

        mockMvc.perform(post("/api/admin/seat-types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_JSON"));
    }
}
