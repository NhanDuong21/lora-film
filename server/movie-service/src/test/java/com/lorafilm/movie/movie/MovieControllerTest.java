package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.movie.controller.MovieController;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MovieController.class,
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
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getMovieDetail_Success() throws Exception {
        MovieDetailDto detailDto = new MovieDetailDto();
        detailDto.setPublicId("movie-uuid");
        detailDto.setSlug("test-movie");
        detailDto.setTitle("Test Movie");
        detailDto.setStatus(MovieStatus.UPCOMING);

        when(movieService.getMovieByIdentifier("movie-uuid")).thenReturn(detailDto);

        mockMvc.perform(get("/api/movies/{movieId}", "movie-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("movie-uuid"))
                .andExpect(jsonPath("$.data.slug").value("test-movie"))
                .andExpect(jsonPath("$.data.title").value("Test Movie"));
    }

    @Test
    void updateMovieStatus_Success() throws Exception {
        MovieDto movieDto = new MovieDto();
        movieDto.setPublicId("movie-uuid");
        movieDto.setSlug("test-movie");
        movieDto.setTitle("Test Movie");
        movieDto.setStatus(MovieStatus.NOW_SHOWING);

        when(movieService.updateMovieStatus(eq("movie-uuid"), eq(MovieStatus.NOW_SHOWING))).thenReturn(movieDto);

        mockMvc.perform(put("/api/admin/movies/{movieId}/status", "movie-uuid")
                        .param("status", "NOW_SHOWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("NOW_SHOWING"));
    }
}
