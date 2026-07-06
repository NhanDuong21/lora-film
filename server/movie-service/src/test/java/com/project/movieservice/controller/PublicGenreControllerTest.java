package com.project.movieservice.controller;

import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.service.GenreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PublicGenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenreService genreService;

    @Test
    void getGenres_ShouldReturnOk_WithoutToken() throws Exception {
        GenreResponse genreResponse = new GenreResponse(1, "Action", "ACTIVE");
        when(genreService.getGenres(false)).thenReturn(Collections.singletonList(genreResponse));

        mockMvc.perform(get("/api/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].genreName").value("Action"));
    }

    @Test
    void getGenreById_ShouldReturnOk_WithoutToken() throws Exception {
        GenreResponse genreResponse = new GenreResponse(1, "Action", "ACTIVE");
        when(genreService.getGenreById(1, false)).thenReturn(genreResponse);

        mockMvc.perform(get("/api/genres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.genreName").value("Action"));
    }
}
