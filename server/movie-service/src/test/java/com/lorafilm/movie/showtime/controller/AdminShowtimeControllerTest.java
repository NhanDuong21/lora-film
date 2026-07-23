package com.lorafilm.movie.showtime.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.request.UpdateShowtimeStatusRequest;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeResponse;
import com.lorafilm.movie.showtime.dto.response.BatchStatusActionSummary;
import com.lorafilm.movie.showtime.dto.response.ShowtimeStatusHistoryResponse;
import com.lorafilm.movie.showtime.service.ShowtimeCommandService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusHistoryService;
import com.lorafilm.movie.showtime.service.ShowtimeStatusTransitionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lorafilm.movie.common.security.SecurityConfig;
import com.lorafilm.movie.common.security.JwtFilter;
import com.lorafilm.movie.common.security.InternalTokenFilter;
import com.lorafilm.movie.common.security.CustomAuthenticationEntryPoint;
import com.lorafilm.movie.common.security.CustomAccessDeniedHandler;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@WebMvcTest(AdminShowtimeController.class)
@Import({AdminShowtimeControllerTest.TestSecurityConfig.class})
class AdminShowtimeControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                    .anyRequest().permitAll()
                );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShowtimeCommandService showtimeCommandService;

    @MockBean
    private ShowtimeStatusTransitionService transitionService;

    @MockBean
    private ShowtimeStatusHistoryService historyService;

    @MockBean
    private com.lorafilm.movie.common.security.JwtProvider jwtProvider;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void transitionStatus_Success() throws Exception {
        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);
        
        AdminShowtimeResponse response = new AdminShowtimeResponse();
        response.setShowtimePublicId("pub-123");
        response.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING.name());

        when(transitionService.transitionStatus(eq("pub-123"), any(UpdateShowtimeStatusRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/showtimes/pub-123/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN_FOR_BOOKING"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void transitionStatus_ForbiddenForCustomer() throws Exception {
        UpdateShowtimeStatusRequest request = new UpdateShowtimeStatusRequest();
        request.setStatus(ShowtimeStatus.OPEN_FOR_BOOKING);

        mockMvc.perform(put("/api/admin/showtimes/pub-123/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getStatusHistory_Success() throws Exception {
        ShowtimeStatusHistoryResponse history = new ShowtimeStatusHistoryResponse();
        history.setNewStatus("DRAFT");

        when(historyService.getShowtimeStatusHistory("pub-123"))
                .thenReturn(List.of(history));

        mockMvc.perform(get("/api/admin/showtimes/pub-123/status-history")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].newStatus").value("DRAFT"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void previewBatchStatus_ReturnsAuthoritativeCounts() throws Exception {
        BatchStatusActionSummary summary = new BatchStatusActionSummary();
        summary.setBatchId("batch-1");
        summary.setTargetStatus("OPEN_FOR_BOOKING");
        summary.setTotalCount(25);
        summary.setEligibleCount(20);
        summary.setAlreadyTargetCount(5);
        summary.setActionAllowed(true);
        summary.setAtomic(true);
        when(transitionService.previewBatchStatus("batch-1", ShowtimeStatus.OPEN_FOR_BOOKING))
                .thenReturn(summary);

        mockMvc.perform(get("/api/admin/showtimes/batch/batch-1/status-preview")
                        .param("targetStatus", "OPEN_FOR_BOOKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(25))
                .andExpect(jsonPath("$.data.eligibleCount").value(20))
                .andExpect(jsonPath("$.data.alreadyTargetCount").value(5))
                .andExpect(jsonPath("$.data.atomic").value(true))
                .andExpect(jsonPath("$.data.actionAllowed").value(true));
    }
}
