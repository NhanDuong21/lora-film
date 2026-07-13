package com.lorafilm.movie.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.dto.CreateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaRequest;
import com.lorafilm.movie.cinema.dto.UpdateCinemaStatusRequest;
import com.lorafilm.movie.cinema.dto.CinemaResponse;
import com.lorafilm.movie.cinema.service.CinemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminCinemaController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        com.lorafilm.movie.common.security.SecurityConfig.class,
                        com.lorafilm.movie.common.security.JwtFilter.class,
                        com.lorafilm.movie.common.security.InternalTokenFilter.class
                }
        ),
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@SuppressWarnings("null")
class AdminCinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CinemaService cinemaService;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void createCinema_Success() throws Exception {
        CreateCinemaRequest request = new CreateCinemaRequest();
        request.setName("Lorafilm District 1");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");
        request.setTimezone("Asia/Ho_Chi_Minh");
        request.setOpenedDate(LocalDate.of(2026, 1, 1));

        CinemaResponse responseDto = new CinemaResponse();
        responseDto.setPublicId("cinema-uuid");
        responseDto.setName(request.getName());
        responseDto.setSlug("lorafilm-district-1");
        responseDto.setStatus(CinemaStatus.DRAFT);
        responseDto.setCity("HCM");
        responseDto.setDistrict("D1");
        responseDto.setAddress("123 Street");

        when(cinemaService.createCinema(any(CreateCinemaRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/cinemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("cinema-uuid"))
                .andExpect(jsonPath("$.data.name").value("Lorafilm District 1"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void createCinema_ValidationError_BlankName() throws Exception {
        CreateCinemaRequest request = new CreateCinemaRequest();
        request.setName(""); // Invalid blank name
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");

        mockMvc.perform(post("/api/admin/cinemas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateCinema_Success() throws Exception {
        UpdateCinemaRequest request = new UpdateCinemaRequest();
        request.setName("Lorafilm Updated");
        request.setCity("HCM");
        request.setDistrict("D1");
        request.setAddress("123 Street");

        CinemaResponse responseDto = new CinemaResponse();
        responseDto.setPublicId("cinema-uuid");
        responseDto.setName("Lorafilm Updated");
        responseDto.setSlug("lorafilm-updated");

        when(cinemaService.updateCinema(eq("cinema-uuid"), any(UpdateCinemaRequest.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/cinemas/cinema-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Lorafilm Updated"));
    }

    @Test
    void updateCinemaStatus_Success() throws Exception {
        UpdateCinemaStatusRequest request = new UpdateCinemaStatusRequest();
        request.setStatus(CinemaStatus.ACTIVE);

        CinemaResponse responseDto = new CinemaResponse();
        responseDto.setPublicId("cinema-uuid");
        responseDto.setStatus(CinemaStatus.ACTIVE);

        when(cinemaService.updateCinemaStatus(eq("cinema-uuid"), eq(CinemaStatus.ACTIVE))).thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/cinemas/cinema-uuid/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
