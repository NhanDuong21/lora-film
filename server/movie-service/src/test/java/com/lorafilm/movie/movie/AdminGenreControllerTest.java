package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.controller.AdminGenreController;
import com.lorafilm.movie.movie.dto.GenreRequest;
import com.lorafilm.movie.movie.dto.GenreResponse;
import com.lorafilm.movie.movie.service.AdminGenreService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminGenreController.class,
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
public class AdminGenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminGenreService adminGenreService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "jpaMappingContext")
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void createGenre_Success() throws Exception {
        GenreRequest request = new GenreRequest();
        request.setName("Action");

        GenreResponse response = new GenreResponse();
        response.setPublicId("genre-id");
        response.setName("Action");
        response.setSlug("action");
        response.setStatus(ActiveStatus.ACTIVE);
        response.setMovieCount(12L);

        when(adminGenreService.createGenre(any(GenreRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value("genre-id"))
                .andExpect(jsonPath("$.data.name").value("Action"))
                .andExpect(jsonPath("$.data.movieCount").value(12));
    }

    @Test
    void createGenre_ValidationFailed() throws Exception {
        GenreRequest request = new GenreRequest();
        // Missing name

        mockMvc.perform(post("/api/admin/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateGenre_Success() throws Exception {
        GenreRequest request = new GenreRequest();
        request.setName("Action Update");

        GenreResponse response = new GenreResponse();
        response.setPublicId("genre-id");
        response.setName("Action Update");
        response.setSlug("action-update");
        response.setStatus(ActiveStatus.ACTIVE);

        when(adminGenreService.updateGenre(eq("genre-id"), any(GenreRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/admin/genres/{publicId}", "genre-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Action Update"));
    }

    @Test
    void updateGenre_NotFound() throws Exception {
        GenreRequest request = new GenreRequest();
        request.setName("Action Update");

        when(adminGenreService.updateGenre(eq("invalid-id"), any(GenreRequest.class)))
                .thenThrow(new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.RESOURCE_NOT_FOUND, "Genre not found", null));

        mockMvc.perform(put("/api/admin/genres/{publicId}", "invalid-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
