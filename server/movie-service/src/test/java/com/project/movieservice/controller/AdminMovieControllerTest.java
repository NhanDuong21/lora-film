package com.project.movieservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.movieservice.dto.*;
import com.project.movieservice.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AdminMovieControllerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private AdminMovieController adminMovieController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminMovieController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAdminMovieDetail_Success() throws Exception {
        AdminMovieDetailResponse response = new AdminMovieDetailResponse();
        response.setId(1L);
        response.setTitle("Test Movie");
        
        when(movieService.getAdminMovieDetail("1")).thenReturn(response);

        mockMvc.perform(get("/api/admin/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Movie"));
    }

    @Test
    void createMovie_Success() throws Exception {
        MovieCreateRequest request = new MovieCreateRequest();
        request.setTitle("New Movie");
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDurationMinutes(120);
        request.setStatus("UPCOMING");
        request.setGenreIds(java.util.Set.of(1));

        MovieCreatedResponse response = new MovieCreatedResponse(1L, "New Movie", "UPCOMING");
        when(movieService.createMovie(any(MovieCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/movies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateMovieStatus_Success() throws Exception {
        MovieStatusUpdateRequest request = new MovieStatusUpdateRequest();
        request.setStatus("NOW_SHOWING");

        MovieStatusResponse response = new MovieStatusResponse(1L, "NOW_SHOWING");
        when(movieService.updateMovieStatus(eq("1"), any(MovieStatusUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/admin/movies/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("NOW_SHOWING"));
    }

    @Test
    void getAdminMovies_Success() throws Exception {
        MoviePageResponse<AdminMovieListItemResponse> response = new MoviePageResponse<>();
        when(movieService.getAdminMovies(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/admin/movies")
                .param("page", "0")
                .param("size", "10")
                .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateMovie_Success() throws Exception {
        MovieUpdateRequest request = new MovieUpdateRequest();
        request.setTitle("Updated Movie");
        request.setReleaseDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(10));
        request.setDurationMinutes(150);
        request.setStatus("NOW_SHOWING");
        request.setGenreIds(java.util.Set.of(1, 2));

        MovieUpdatedResponse response = new MovieUpdatedResponse(1L, "Updated Movie", "NOW_SHOWING");
        when(movieService.updateMovie(eq("1"), any(MovieUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/movies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Updated Movie"));
    }
}
