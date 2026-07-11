package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.controller.MovieVersionController;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.dto.CreateMovieVersionRequest;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieVersionRequest;
import com.lorafilm.movie.movie.service.MovieVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MovieVersionController.class,
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
class MovieVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovieVersionService movieVersionService;

    // Mock to bypass jpaMappingContext error when @EnableJpaAuditing is on main class
    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void getActiveVersions_Success() throws Exception {
        MovieVersionResponse version = new MovieVersionResponse(
                "version-uuid",
                "2D Vietsub",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(movieVersionService.getActiveVersionsByMovie("movie-uuid"))
                .thenReturn(Collections.singletonList(version));

        mockMvc.perform(get("/api/movies/{movieId}/versions", "movie-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data[0].publicId").value("version-uuid"))
                .andExpect(jsonPath("$.data[0].versionName").value("2D Vietsub"));
    }

    @Test
    void createVersion_Success() throws Exception {
        CreateMovieVersionRequest request = new CreateMovieVersionRequest(
                "2D Vietsub",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE
        );

        MovieVersionResponse version = new MovieVersionResponse(
                "version-uuid",
                "2D Vietsub",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(movieVersionService.createVersion(eq("movie-uuid"), any(CreateMovieVersionRequest.class)))
                .thenReturn(version);

        mockMvc.perform(post("/api/admin/movies/{movieId}/versions", "movie-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("version-uuid"))
                .andExpect(jsonPath("$.data.versionName").value("2D Vietsub"));
    }

    @Test
    void createVersion_InvalidInput_ReturnsBadRequest() throws Exception {
        CreateMovieVersionRequest request = new CreateMovieVersionRequest(
                "", // Blank versionName
                null, // Null format
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE
        );

        mockMvc.perform(post("/api/admin/movies/{movieId}/versions", "movie-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateVersion_Success() throws Exception {
        UpdateMovieVersionRequest request = new UpdateMovieVersionRequest(
                "2D Vietsub Updated",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.INACTIVE
        );

        MovieVersionResponse version = new MovieVersionResponse(
                "version-uuid",
                "2D Vietsub Updated",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.INACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(movieVersionService.updateVersion(eq("version-uuid"), any(UpdateMovieVersionRequest.class)))
                .thenReturn(version);

        mockMvc.perform(put("/api/admin/movie-versions/{versionId}", "version-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.versionName").value("2D Vietsub Updated"));
    }

    @Test
    void getAllVersions_Success() throws Exception {
        MovieVersionResponse activeVersion = new MovieVersionResponse(
                "version-uuid-1",
                "2D Vietsub",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        MovieVersionResponse inactiveVersion = new MovieVersionResponse(
                "version-uuid-2",
                "3D Dubbed",
                MovieFormat.THREE_D,
                "EN",
                "NONE",
                "VI",
                ActiveStatus.INACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(movieVersionService.getAllVersionsByMovie("movie-uuid"))
                .thenReturn(List.of(activeVersion, inactiveVersion));

        mockMvc.perform(get("/api/admin/movies/{movieId}/versions", "movie-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].publicId").value("version-uuid-1"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[1].publicId").value("version-uuid-2"))
                .andExpect(jsonPath("$.data[1].status").value("INACTIVE"));
    }
}
