package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.movie.controller.AdminMovieController;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieGenreAssignRequest;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.service.AdminMovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminMovieController.class,
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
public class AdminMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminMovieService adminMovieService;

    @MockBean
    private com.lorafilm.movie.movie.service.MovieService movieService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void createMovie_Success() throws Exception {
        MovieRequest request = new MovieRequest();
        request.setTitle("New Movie");
        request.setDurationMinutes(120);
        request.setAgeRating(AgeRating.T13);
        request.setReleaseDate(LocalDate.now());

        MovieDto responseDto = new MovieDto();
        responseDto.setPublicId("public-id");
        responseDto.setTitle("New Movie");
        responseDto.setStatus(MovieStatus.DRAFT);

        when(adminMovieService.createMovie(any(MovieRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("public-id"))
                .andExpect(jsonPath("$.data.title").value("New Movie"));
    }

    @Test
    void createMovie_ValidationFailed() throws Exception {
        MovieRequest request = new MovieRequest();
        // Missing title, duration <= 0
        request.setDurationMinutes(-10);

        mockMvc.perform(post("/api/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateMovie_Success() throws Exception {
        MovieRequest request = new MovieRequest();
        request.setTitle("Updated Movie");
        request.setDurationMinutes(150);
        request.setAgeRating(AgeRating.T16);
        request.setReleaseDate(LocalDate.now());
        request.setStatus(MovieStatus.UPCOMING);

        MovieDto responseDto = new MovieDto();
        responseDto.setPublicId("public-id");
        responseDto.setTitle("Updated Movie");
        responseDto.setStatus(MovieStatus.UPCOMING);

        when(adminMovieService.updateMovie(eq("public-id"), any(MovieRequest.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/movies/{publicId}", "public-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Movie"));
    }

    @Test
    void assignGenres_Success() throws Exception {
        MovieGenreAssignRequest request = new MovieGenreAssignRequest();
        request.setGenreIds(List.of("genre-id-1", "genre-id-2"));

        doNothing().when(adminMovieService).assignGenres(eq("public-id"), any());

        mockMvc.perform(post("/api/admin/movies/{publicId}/genres", "public-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Genres assigned successfully"));
    }

    @Test
    void updateMovie_NotFound() throws Exception {
        MovieRequest request = new MovieRequest();
        request.setTitle("Updated Movie");
        request.setDurationMinutes(150);
        request.setAgeRating(AgeRating.T16);
        request.setReleaseDate(LocalDate.now());

        when(adminMovieService.updateMovie(eq("invalid-id"), any(MovieRequest.class)))
                .thenThrow(new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));

        mockMvc.perform(put("/api/admin/movies/{publicId}", "invalid-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MOVIE_NOT_FOUND"));
    }

    @Test
    void updateMovie_PublishValidationFailed() throws Exception {
        MovieRequest request = new MovieRequest();
        request.setTitle("Updated Movie");
        request.setDurationMinutes(150);
        request.setAgeRating(AgeRating.T16);
        request.setReleaseDate(LocalDate.now());
        request.setStatus(MovieStatus.NOW_SHOWING);

        when(adminMovieService.updateMovie(eq("public-id"), any(MovieRequest.class)))
                .thenThrow(new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least 1 genre", null));

        mockMvc.perform(put("/api/admin/movies/{publicId}", "public-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MOVIE_PUBLISH_VALIDATION_FAILED"));
    }

    @Test
    void assignGenresPut_Success() throws Exception {
        MovieGenreAssignRequest request = new MovieGenreAssignRequest();
        request.setGenreIds(List.of("genre-id-1"));

        doNothing().when(adminMovieService).assignGenres(eq("public-id"), any());

        mockMvc.perform(put("/api/admin/movies/{publicId}/genres", "public-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Genres updated successfully"));
    }
}
