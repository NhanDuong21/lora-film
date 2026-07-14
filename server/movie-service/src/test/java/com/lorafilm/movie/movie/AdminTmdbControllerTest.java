package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.movie.controller.AdminTmdbController;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.tmdb.TmdbApproveRequest;
import com.lorafilm.movie.movie.service.TmdbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminTmdbController.class,
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
public class AdminTmdbControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TmdbService tmdbService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void approveTmdbMovie_Success() throws Exception {
        TmdbApproveRequest request = new TmdbApproveRequest();
        request.setTmdbId(533535);

        MovieDto response = new MovieDto();
        response.setPublicId("movie-1");
        response.setTitle("Deadpool & Wolverine");
        response.setStatus(MovieStatus.DRAFT);

        when(tmdbService.approveTmdbMovie(eq(533535))).thenReturn(response);

        mockMvc.perform(post("/api/admin/tmdb/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("movie-1"))
                .andExpect(jsonPath("$.data.title").value("Deadpool & Wolverine"));
    }

    @Test
    void approveTmdbMovie_MissingId() throws Exception {
        TmdbApproveRequest request = new TmdbApproveRequest();
        // missing tmdbId

        mockMvc.perform(post("/api/admin/tmdb/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void approveTmdbMovie_ApiError() throws Exception {
        TmdbApproveRequest request = new TmdbApproveRequest();
        request.setTmdbId(999999);

        when(tmdbService.approveTmdbMovie(eq(999999)))
                .thenThrow(new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.INTERNAL_SERVER_ERROR, "TMDB API Error", null));

        mockMvc.perform(post("/api/admin/tmdb/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"));
    }
}
