package com.lorafilm.movie.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CinemaController.class,
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
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CinemaService cinemaService;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getCinemaMedia_Success() throws Exception {
        CinemaDetailDto.CinemaMediaDto mediaDto = new CinemaDetailDto.CinemaMediaDto();
        mediaDto.setPublicId("media-uuid");
        mediaDto.setUrl("http://example.com/image.jpg");

        when(cinemaService.getCinemaMedia(eq("cinema-uuid")))
                .thenReturn(Collections.singletonList(mediaDto));

        mockMvc.perform(get("/api/cinemas/cinema-uuid/media")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].publicId").value("media-uuid"));
    }

    @Test
    void getCinemaOperatingHours_Success() throws Exception {
        CinemaDetailDto.OperatingHourDto hourDto = new CinemaDetailDto.OperatingHourDto();
        hourDto.setDayOfWeek(1);
        hourDto.setOpenTime("08:00:00");
        hourDto.setCloseTime("22:00:00");
        hourDto.setIsClosed(false);

        when(cinemaService.getCinemaOperatingHours(eq("cinema-uuid")))
                .thenReturn(Collections.singletonList(hourDto));

        mockMvc.perform(get("/api/cinemas/cinema-uuid/operating-hours")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].dayOfWeek").value(1));
    }
}
