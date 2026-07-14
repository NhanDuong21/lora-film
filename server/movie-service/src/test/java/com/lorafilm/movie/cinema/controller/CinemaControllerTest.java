package com.lorafilm.movie.cinema.controller;

import com.lorafilm.movie.cinema.dto.CinemaDetailDto;
import com.lorafilm.movie.cinema.dto.CinemaDto;
import com.lorafilm.movie.cinema.dto.CinemaClosurePeriodResponse;
import com.lorafilm.movie.cinema.service.CinemaService;
import com.lorafilm.movie.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    void getCinemas_Success() throws Exception {
        CinemaDto cinemaDto = new CinemaDto();
        cinemaDto.setPublicId("cinema-uuid");
        cinemaDto.setName("Public Cinema");

        PageResponse<CinemaDto> pageResponse = new PageResponse<>(
                Collections.singletonList(cinemaDto), 0, 10, 1L, 1, true);

        when(cinemaService.getCinemas(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/cinemas")
                        .param("city", "HCM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].publicId").value("cinema-uuid"));
    }

    @Test
    void getCinemaDetail_Success() throws Exception {
        CinemaDetailDto detailDto = new CinemaDetailDto();
        detailDto.setPublicId("cinema-uuid");
        detailDto.setName("Public Cinema Detail");

        when(cinemaService.getCinemaByIdentifier("cinema-uuid")).thenReturn(detailDto);

        mockMvc.perform(get("/api/cinemas/cinema-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("cinema-uuid"))
                .andExpect(jsonPath("$.data.name").value("Public Cinema Detail"));
    }

    @Test
    void getCinemaClosurePeriods_Success() throws Exception {
        CinemaClosurePeriodResponse closureDto = new CinemaClosurePeriodResponse();
        closureDto.setId(301L);
        closureDto.setCinemaPublicId("cinema-uuid");

        when(cinemaService.getCinemaClosurePeriods("cinema-uuid"))
                .thenReturn(Collections.singletonList(closureDto));

        mockMvc.perform(get("/api/cinemas/cinema-uuid/closure-periods")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(301));
    }
}
