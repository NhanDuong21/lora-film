package com.lorafilm.movie.movie;

import com.lorafilm.movie.movie.controller.AdminMovieController;
import com.lorafilm.movie.movie.service.AdminMovieService;
import com.lorafilm.movie.movie.service.MovieService;
import com.lorafilm.movie.movie.service.MovieSummaryQueryService;
import com.lorafilm.movie.integration.tmdb.service.TmdbMovieReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminMovieController.class)
@Import(AdminMovieSummarySecurityTest.TestSecurityConfig.class)
class AdminMovieSummarySecurityTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                            .anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockBean private AdminMovieService adminMovieService;
    @MockBean private MovieService movieService;
    @MockBean private MovieSummaryQueryService movieSummaryQueryService;
    @MockBean private TmdbMovieReviewService tmdbMovieReviewService;
    @MockBean private com.lorafilm.movie.common.security.JwtProvider jwtProvider;

    @Test
    void summaryRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void summaryRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void summaryAllowsAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/summary"))
                .andExpect(status().isOk());
    }

    @Test
    void tmdbReviewRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/movie-id/tmdb-review"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void tmdbReviewRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/movie-id/tmdb-review"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void tmdbReviewAllowsAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/movie-id/tmdb-review"))
                .andExpect(status().isOk());
    }

    @Test
    void bulkApprovalRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(post("/api/admin/movies/bulk-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\",\"source\":\"TMDB\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void bulkApprovalAllowsAdminUsers() throws Exception {
        mockMvc.perform(post("/api/admin/movies/bulk-approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\",\"source\":\"TMDB\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void queueBreakdownRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/tmdb-queue-breakdown")
                        .param("status", "DRAFT")
                        .param("source", "TMDB"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void queueBreakdownAllowsAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/movies/tmdb-queue-breakdown")
                        .param("status", "DRAFT")
                        .param("source", "TMDB"))
                .andExpect(status().isOk());
    }
}
