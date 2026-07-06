package com.project.movieservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.movieservice.dto.GenreCreateRequest;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.dto.GenreUpdateRequest;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.service.GenreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminGenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GenreService genreService;

    @Test
    void createGenre_ShouldReturn401_WhenNoToken() throws Exception {
        GenreCreateRequest request = new GenreCreateRequest("Action");

        mockMvc.perform(post("/api/admin/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void createGenre_ShouldReturn403_WhenNotAdmin() throws Exception {
        GenreCreateRequest request = new GenreCreateRequest("Action");

        mockMvc.perform(post("/api/admin/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createGenre_ShouldReturnCreated_WhenAdmin() throws Exception {
        GenreCreateRequest request = new GenreCreateRequest("Action");
        GenreResponse response = new GenreResponse(1, "Action", "ACTIVE");
        when(genreService.createGenre(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.genreName").value("Action"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createGenre_ShouldReturn400_WhenInvalidRequest() throws Exception {
        GenreCreateRequest request = new GenreCreateRequest("");

        mockMvc.perform(post("/api/admin/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void createGenre_ShouldReturn409_WhenDuplicate() throws Exception {
        GenreCreateRequest request = new GenreCreateRequest("Action");
        when(genreService.createGenre(any())).thenThrow(new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/admin/genres")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GENRE_ALREADY_EXISTS"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateGenre_ShouldReturnOk_WhenAdmin() throws Exception {
        GenreUpdateRequest request = new GenreUpdateRequest("Sci-Fi", "ACTIVE");
        GenreResponse response = new GenreResponse(1, "Sci-Fi", "ACTIVE");
        when(genreService.updateGenre(eq(1), any())).thenReturn(response);

        mockMvc.perform(put("/api/admin/genres/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.genreName").value("Sci-Fi"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void updateGenre_ShouldReturn404_WhenNotFound() throws Exception {
        GenreUpdateRequest request = new GenreUpdateRequest("Sci-Fi", "ACTIVE");
        when(genreService.updateGenre(eq(1), any())).thenThrow(new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/admin/genres/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GENRE_NOT_FOUND"));
    }
}
