package com.lorafilm.movie.cinema.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.domain.enums.CinemaMediaType;
import com.lorafilm.movie.cinema.dto.*;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.cinema.service.CinemaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lorafilm.movie.common.dto.PageResponse;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCinemaController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        com.lorafilm.movie.common.security.SecurityConfig.class,
        com.lorafilm.movie.common.security.JwtFilter.class,
        com.lorafilm.movie.common.security.InternalTokenFilter.class
}), excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
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

    @Test
    void addCinemaMedia_Success() throws Exception {
        CreateCinemaMediaRequest request = new CreateCinemaMediaRequest();
        request.setMediaType(CinemaMediaType.BANNER);
        request.setUrl("http://example.com/banner.jpg");
        request.setTitle("Banner Title");
        request.setIsPrimary(true);

        CinemaMediaResponse responseDto = new CinemaMediaResponse();
        responseDto.setPublicId("media-uuid");
        responseDto.setMediaType(CinemaMediaType.BANNER);
        responseDto.setUrl(request.getUrl());
        responseDto.setTitle(request.getTitle());
        responseDto.setIsPrimary(true);

        when(cinemaService.addCinemaMedia(eq("cinema-uuid"), any(CreateCinemaMediaRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/cinemas/cinema-uuid/media")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("media-uuid"))
                .andExpect(jsonPath("$.data.isPrimary").value(true));
    }

    @Test
    void updateOperatingHours_Success() throws Exception {
        List<OperatingHourUpdateRequest> requests = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            OperatingHourUpdateRequest req = new OperatingHourUpdateRequest();
            req.setDayOfWeek(i);
            req.setIsClosed(false);
            req.setOpenTime(LocalTime.of(8, 0));
            req.setCloseTime(LocalTime.of(22, 0));
            requests.add(req);
        }

        List<OperatingHourResponse> responses = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            OperatingHourResponse resp = new OperatingHourResponse();
            resp.setDayOfWeek(i);
            resp.setIsClosed(false);
            resp.setOpenTime(LocalTime.of(8, 0));
            resp.setCloseTime(LocalTime.of(22, 0));
            responses.add(resp);
        }

        when(cinemaService.updateOperatingHours(eq("cinema-uuid"), anyList())).thenReturn(responses);

        mockMvc.perform(put("/api/admin/cinemas/cinema-uuid/operating-hours")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(7));
    }

    @Test
    void createClosurePeriod_Success() throws Exception {
        CreateCinemaClosurePeriodRequest request = new CreateCinemaClosurePeriodRequest();
        request.setStartTime(Instant.parse("2026-12-01T00:00:00Z"));
        request.setEndTime(Instant.parse("2026-12-02T00:00:00Z"));
        request.setReason("Renovation");

        CinemaClosurePeriodResponse responseDto = new CinemaClosurePeriodResponse();
        responseDto.setId(101L);
        responseDto.setCinemaPublicId("cinema-uuid");
        responseDto.setStartTime(request.getStartTime());
        responseDto.setEndTime(request.getEndTime());
        responseDto.setStatus(ActionStatus.ACTIVE);

        when(cinemaService.createClosurePeriod(eq("cinema-uuid"), any(CreateCinemaClosurePeriodRequest.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/cinemas/cinema-uuid/closure-periods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void cancelClosurePeriod_Success() throws Exception {
        CinemaClosurePeriodResponse responseDto = new CinemaClosurePeriodResponse();
        responseDto.setId(101L);
        responseDto.setStatus(ActionStatus.CANCELLED);

        when(cinemaService.cancelClosurePeriod(eq(101L))).thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/closure-periods/101/cancel")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void getAdminCinemas_Success() throws Exception {
        CinemaResponse cinemaResponse = new CinemaResponse();
        cinemaResponse.setPublicId("cinema-uuid");
        cinemaResponse.setName("Admin Cinema");
        cinemaResponse.setStatus(CinemaStatus.DRAFT);

        PageResponse<CinemaResponse> pageResponse = new PageResponse<>(
                List.of(cinemaResponse), 0, 10, 1L, 1, true);

        when(cinemaService.getAdminCinemas(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/cinemas")
                .param("status", "DRAFT")
                .param("showDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].publicId").value("cinema-uuid"))
                .andExpect(jsonPath("$.data.data[0].status").value("DRAFT"));
    }

    @Test
    void getAdminCinemaDetail_Success() throws Exception {
        CinemaDetailDto responseDto = new CinemaDetailDto();
        responseDto.setPublicId("cinema-uuid");
        responseDto.setName("Admin Cinema Detail");

        when(cinemaService.getAdminCinemaDetail("cinema-uuid")).thenReturn(responseDto);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/cinemas/cinema-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("cinema-uuid"))
                .andExpect(jsonPath("$.data.name").value("Admin Cinema Detail"));
    }

    @Test
    void deleteCinema_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/cinemas/cinema-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteCinemaMedia_Success() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/cinema-media/media-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAdminCinemaClosurePeriods_Success() throws Exception {
        CinemaClosurePeriodResponse response = new CinemaClosurePeriodResponse();
        response.setId(200L);
        response.setCinemaPublicId("cinema-uuid");
        response.setStatus(ActionStatus.ACTIVE);

        PageResponse<CinemaClosurePeriodResponse> pageResponse = new PageResponse<>(
                List.of(response), 0, 10, 1L, 1, true);

        when(cinemaService.getAdminCinemaClosurePeriods(eq("cinema-uuid"), any(), any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/cinemas/cinema-uuid/closure-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].id").value(200));
    }
}
