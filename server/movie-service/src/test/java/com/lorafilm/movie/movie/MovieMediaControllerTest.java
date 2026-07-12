package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.controller.MovieMediaController;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.dto.CreateMovieMediaRequest;
import com.lorafilm.movie.movie.dto.MovieMediaResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieMediaRequest;
import com.lorafilm.movie.movie.service.MovieMediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MovieMediaController.class,
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
class MovieMediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovieMediaService movieMediaService;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getCustomerMedia_Success() throws Exception {
        MovieMediaResponse media = new MovieMediaResponse(
                "media-uuid",
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaService.getCustomerMedia("movie-uuid"))
                .thenReturn(Collections.singletonList(media));

        mockMvc.perform(get("/api/movies/{movieId}/media", "movie-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].publicId").value("media-uuid"))
                .andExpect(jsonPath("$.data[0].url").value("http://example.com/poster.jpg"));
    }

    @Test
    void createMedia_Success() throws Exception {
        CreateMovieMediaRequest request = new CreateMovieMediaRequest(
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        MovieMediaResponse response = new MovieMediaResponse(
                "media-uuid",
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaService.createMedia(eq("movie-uuid"), any(CreateMovieMediaRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/admin/movies/{movieId}/media", "movie-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("media-uuid"));
    }

    @Test
    void createMedia_InvalidInput_ReturnsBadRequest() throws Exception {
        CreateMovieMediaRequest request = new CreateMovieMediaRequest(
                null, // null media type
                "", // blank URL
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        mockMvc.perform(post("/api/admin/movies/{movieId}/media", "movie-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMedia_Success() throws Exception {
        UpdateMovieMediaRequest request = new UpdateMovieMediaRequest(
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        MovieMediaResponse response = new MovieMediaResponse(
                "media-uuid",
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaService.updateMedia(eq("media-uuid"), any(UpdateMovieMediaRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/admin/movie-media/{mediaId}", "media-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("media-uuid"));
    }

    @Test
    void deleteMedia_Success() throws Exception {
        doNothing().when(movieMediaService).deleteMedia("media-uuid");

        mockMvc.perform(delete("/api/admin/movie-media/{mediaId}", "media-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Movie media deleted successfully"));
    }

    @Test
    void getAdminMedia_Success() throws Exception {
        MovieMediaResponse media = new MovieMediaResponse(
                "media-uuid",
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaService.getMovieMedia("movie-uuid"))
                .thenReturn(Collections.singletonList(media));

        mockMvc.perform(get("/api/admin/movies/{movieId}/media", "movie-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].publicId").value("media-uuid"));
    }

    @Test
    void getMedia_Success() throws Exception {
        MovieMediaResponse media = new MovieMediaResponse(
                "media-uuid",
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaService.getMedia("media-uuid")).thenReturn(media);

        mockMvc.perform(get("/api/admin/movie-media/{mediaId}", "media-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("media-uuid"))
                .andExpect(jsonPath("$.data.url").value("http://example.com/poster.jpg"));
    }
}
