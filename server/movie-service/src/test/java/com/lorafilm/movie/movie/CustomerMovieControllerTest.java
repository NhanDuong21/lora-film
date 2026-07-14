package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.movie.controller.CustomerMovieController;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.service.CustomerMovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CustomerMovieController.class,
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
public class CustomerMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerMovieService customerMovieService;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getMovies_NowShowing_Success() throws Exception {
        MovieDto movie = new MovieDto();
        movie.setPublicId("movie-1");
        movie.setTitle("Now Showing Movie");
        
        PageResponse<MovieDto> pageResponse = PageResponse.of(new PageImpl<>(List.of(movie), PageRequest.of(0, 10), 1), List.of(movie));
        
        when(customerMovieService.getMoviesByStatus(eq("now-showing"), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/customer/movies")
                        .param("status", "now-showing")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].publicId").value("movie-1"))
                .andExpect(jsonPath("$.data.content[0].title").value("Now Showing Movie"));
    }

    @Test
    void getMovieDetail_Success() throws Exception {
        com.lorafilm.movie.movie.dto.MovieDetailDto movie = new com.lorafilm.movie.movie.dto.MovieDetailDto();
        movie.setPublicId("movie-1");
        movie.setTitle("Movie Detail");
        
        when(customerMovieService.getMovieDetail("movie-1")).thenReturn(movie);

        mockMvc.perform(get("/api/customer/movies/{identifier}", "movie-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("movie-1"))
                .andExpect(jsonPath("$.data.title").value("Movie Detail"));
    }

    @Test
    void getMovies_ComingSoon_Success() throws Exception {
        MovieDto movie = new MovieDto();
        movie.setPublicId("movie-2");
        movie.setTitle("Coming Soon Movie");
        
        PageResponse<MovieDto> pageResponse = PageResponse.of(new PageImpl<>(List.of(movie), PageRequest.of(0, 10), 1), List.of(movie));
        
        when(customerMovieService.getMoviesByStatus(eq("coming-soon"), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/customer/movies")
                        .param("status", "coming-soon")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].publicId").value("movie-2"))
                .andExpect(jsonPath("$.data.content[0].title").value("Coming Soon Movie"));
    }

    @Test
    void getMovies_InvalidStatus() throws Exception {
        when(customerMovieService.getMoviesByStatus(eq("invalid-status"), any(), any()))
                .thenThrow(new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.VALIDATION_ERROR, "Invalid status parameter", null));

        mockMvc.perform(get("/api/customer/movies")
                        .param("status", "invalid-status")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void getMovieDetail_NotFound() throws Exception {
        when(customerMovieService.getMovieDetail("invalid-id"))
                .thenThrow(new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));

        mockMvc.perform(get("/api/customer/movies/{identifier}", "invalid-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MOVIE_NOT_FOUND"));
    }
}
