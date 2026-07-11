package com.project.analyticsservice.controller;

import com.project.analyticsservice.common.ApiResponse;
import com.project.analyticsservice.dto.*;
import com.project.analyticsservice.service.MovieAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class MovieAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieAnalyticsService movieAnalyticsService;

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void getMovieRevenueList_Admin_Success() throws Exception {
        MovieRevenueListResponse response = new MovieRevenueListResponse(
                "LIFETIME", null, Collections.emptyList(), 0, 10, 0L, 0, true, true);

        when(movieAnalyticsService.getMovieRevenueList(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movie revenue statistics retrieved successfully"));
    }

    @Test
    @WithMockUser(username = "manager", authorities = {"ROLE_MANAGER"})
    void getMovieRevenueList_Manager_Success() throws Exception {
        MovieRevenueListResponse response = new MovieRevenueListResponse(
                "LIFETIME", null, Collections.emptyList(), 0, 10, 0L, 0, true, true);

        when(movieAnalyticsService.getMovieRevenueList(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "customer", authorities = {"ROLE_CUSTOMER"})
    void getMovieRevenueList_Customer_Forbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/movies"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void getMovieRevenueList_Anonymous_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/analytics/movies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void getMovieRevenueDetail_Admin_Success() throws Exception {
        MovieRevenueDetailResponse response = new MovieRevenueDetailResponse(
                101L, "Avengers", "LIFETIME", null, null, 850,
                new BigDecimal("98500000.00"), new BigDecimal("115882.35"), "VND", LocalDateTime.now());

        when(movieAnalyticsService.getMovieRevenueDetail(any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/movies/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movieId").value(101))
                .andExpect(jsonPath("$.data.movieTitle").value("Avengers"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void getMovieRevenueTrend_Admin_Success() throws Exception {
        MovieRevenueTrendResponse response = new MovieRevenueTrendResponse(
                101L, "Avengers", "2026-06-19", "2026-06-21", "VND", Collections.emptyList());

        when(movieAnalyticsService.getMovieRevenueTrend(any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/movies/101/trend?startDate=2026-06-19&endDate=2026-06-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.movieId").value(101));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void getTopMovies_Admin_Success() throws Exception {
        TopMoviesResponse response = new TopMoviesResponse(
                "REVENUE", "LIFETIME", null, "VND", Collections.emptyList(), LocalDateTime.now());

        when(movieAnalyticsService.getTopMovies(any(), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/analytics/movies/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metric").value("REVENUE"));
    }
}
