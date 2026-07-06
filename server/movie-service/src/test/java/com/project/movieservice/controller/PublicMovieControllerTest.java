package com.project.movieservice.controller;

import com.project.movieservice.dto.MovieDetailResponse;
import com.project.movieservice.dto.MovieListItemResponse;
import com.project.movieservice.dto.MoviePageResponse;
import com.project.movieservice.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    @Test
    void testGetMovies() throws Exception {
        MoviePageResponse<MovieListItemResponse> response = new MoviePageResponse<>(
                Collections.emptyList(), 0, 10, 0, 0, true, true);
                
        when(movieService.getMovies(any(), any(), any(), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn((MoviePageResponse) response);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetMovieDetail() throws Exception {
        MovieDetailResponse response = new MovieDetailResponse();
        response.setId(1L);
        response.setTitle("Avengers");

        when(movieService.getMovieDetail("1", false)).thenReturn(response);

        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Avengers"));
    }
}
