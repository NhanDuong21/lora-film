package com.lorafilm.movie.movie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.movie.controller.AdminMovieController;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.AdminMovieListQuery;
import com.lorafilm.movie.movie.dto.MovieBulkApprovalResponse;
import com.lorafilm.movie.movie.dto.MovieBulkApprovalResult;
import com.lorafilm.movie.movie.dto.MovieSummaryResponse;
import com.lorafilm.movie.movie.dto.MovieGenreAssignRequest;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.service.AdminMovieService;
import com.lorafilm.movie.movie.service.MovieSummaryQueryService;
import com.lorafilm.movie.integration.tmdb.service.TmdbMovieReviewService;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieReviewResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbFieldDiffDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbCollectionDiffDto;
import com.lorafilm.movie.movie.dto.MovieReadinessDto;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
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
import static org.mockito.ArgumentMatchers.argThat;
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

    @MockBean
    private MovieSummaryQueryService movieSummaryQueryService;

    @MockBean
    private TmdbMovieReviewService tmdbMovieReviewService;

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
                .andExpect(jsonPath("$.message").value("Genres appended successfully"));
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
    void genericUpdate_WithStatusInRawJson_MustNotChangeLifecycle() throws Exception {
        String rawJson = """
            {
                "title": "Updated title",
                "durationMinutes": 150,
                "ageRating": "T16",
                "releaseDate": "2026-07-21",
                "status": "NOW_SHOWING"
            }
        """;

        MovieDto responseDto = new MovieDto();
        responseDto.setPublicId("public-id");
        responseDto.setTitle("Updated title");
        responseDto.setStatus(MovieStatus.DRAFT);

        when(adminMovieService.updateMovie(eq("public-id"), any(MovieRequest.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/admin/movies/{publicId}", "public-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
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

    @Test
    void getMovies_Success_ReturnAllIncludingDraft() throws Exception {
        com.lorafilm.movie.common.dto.PageResponse<MovieDto> pageResponse = new com.lorafilm.movie.common.dto.PageResponse<>();
        MovieDto draftMovie = new MovieDto();
        draftMovie.setPublicId("movie-1");
        draftMovie.setTitle("Draft Movie");
        draftMovie.setStatus(MovieStatus.DRAFT);
        pageResponse.setData(List.of(draftMovie));
        pageResponse.setPageNo(0);
        pageResponse.setPageSize(10);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);
        pageResponse.setLast(true);

        when(movieService.getMovies(any(AdminMovieListQuery.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].publicId").value("movie-1"))
                .andExpect(jsonPath("$.data.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void bulkApproveTmdbMovies_ReturnsPerMovieResults() throws Exception {
        MovieBulkApprovalResponse response = new MovieBulkApprovalResponse(
                2,
                1,
                1,
                0,
                100,
                List.of(
                        MovieBulkApprovalResult.approved("movie-1", "Ready Movie", MovieStatus.UPCOMING),
                        MovieBulkApprovalResult.skipped(
                                "movie-2",
                                "Blocked Movie",
                                "MOVIE_PRIMARY_POSTER_REQUIRED",
                                "Primary poster is required")));
        when(movieService.bulkApproveTmdbMovies(any(AdminMovieListQuery.class), eq(100))).thenReturn(response);

        mockMvc.perform(post("/api/admin/movies/bulk-approve")
                        .param("limit", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DRAFT",
                                  "source": "TMDB",
                                  "healthStatus": "READY",
                                  "sort": "releaseDate,desc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requested").value(2))
                .andExpect(jsonPath("$.data.approved").value(1))
                .andExpect(jsonPath("$.data.skipped").value(1))
                .andExpect(jsonPath("$.data.results[0].outcome").value("APPROVED"))
                .andExpect(jsonPath("$.data.results[1].outcome").value("SKIPPED"))
                .andExpect(jsonPath("$.data.results[1].reasonCode").value("MOVIE_PRIMARY_POSTER_REQUIRED"));
    }

    @Test
    void getMovies_EmptyPage_ReturnsEmptyListNotError() throws Exception {
        com.lorafilm.movie.common.dto.PageResponse<MovieDto> pageResponse = new com.lorafilm.movie.common.dto.PageResponse<>();
        pageResponse.setData(List.of());
        pageResponse.setPageNo(0);
        pageResponse.setPageSize(10);
        pageResponse.setTotalElements(0);
        pageResponse.setTotalPages(0);
        pageResponse.setLast(true);

        when(movieService.getMovies(any(AdminMovieListQuery.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getMovies_WithStatusFilter() throws Exception {
        com.lorafilm.movie.common.dto.PageResponse<MovieDto> pageResponse = new com.lorafilm.movie.common.dto.PageResponse<>();
        MovieDto movie = new MovieDto();
        movie.setPublicId("movie-2");
        movie.setTitle("Showing Movie");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        pageResponse.setData(List.of(movie));
        pageResponse.setPageNo(0);
        pageResponse.setPageSize(10);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);
        pageResponse.setLast(true);

        when(movieService.getMovies(argThat(query -> "NOW_SHOWING".equals(query.getStatus()))))
                .thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("status", "NOW_SHOWING")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].publicId").value("movie-2"))
                .andExpect(jsonPath("$.data.data[0].status").value("NOW_SHOWING"));
    }

    @Test
    void getMovies_WithKeywordFilter() throws Exception {
        com.lorafilm.movie.common.dto.PageResponse<MovieDto> pageResponse = new com.lorafilm.movie.common.dto.PageResponse<>();
        MovieDto movie = new MovieDto();
        movie.setPublicId("movie-3");
        movie.setTitle("Batman");
        movie.setStatus(MovieStatus.NOW_SHOWING);
        pageResponse.setData(List.of(movie));
        pageResponse.setPageNo(0);
        pageResponse.setPageSize(10);
        pageResponse.setTotalElements(1);
        pageResponse.setTotalPages(1);
        pageResponse.setLast(true);

        when(movieService.getMovies(argThat(query -> "Batman".equals(query.getKeyword()))))
                .thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("keyword", "Batman")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].publicId").value("movie-3"))
                .andExpect(jsonPath("$.data.data[0].title").value("Batman"));
    }

    @Test
    void getMovieSummary_ReturnsExactContract() throws Exception {
        MovieSummaryResponse summary = new MovieSummaryResponse(
                15, 3, 3, 3, 3, 3, 8, 4, 3, 5, 4, 7);
        when(movieSummaryQueryService.getSummary()).thenReturn(summary);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/admin/movies/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errorCode").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.total").value(15))
                .andExpect(jsonPath("$.data.draft").value(3))
                .andExpect(jsonPath("$.data.upcoming").value(3))
                .andExpect(jsonPath("$.data.nowShowing").value(3))
                .andExpect(jsonPath("$.data.ended").value(3))
                .andExpect(jsonPath("$.data.inactive").value(3))
                .andExpect(jsonPath("$.data.ready").value(8))
                .andExpect(jsonPath("$.data.warning").value(4))
                .andExpect(jsonPath("$.data.blocked").value(3))
                .andExpect(jsonPath("$.data.missingPrimaryPoster").value(5))
                .andExpect(jsonPath("$.data.missingActiveVersion").value(4))
                .andExpect(jsonPath("$.data.withoutShowtime").value(7))
                .andExpect(jsonPath("$.data.*", org.hamcrest.Matchers.hasSize(12)))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getTmdbReview_ReturnsCanonicalReadOnlyContract() throws Exception {
        TmdbMovieReviewResponse review = new TmdbMovieReviewResponse(
                "TMDB",
                501L,
                "PENDING",
                true,
                List.of(),
                new MovieReadinessDto(MovieHealthStatus.READY, "READY", List.of(), List.of()),
                java.time.LocalDateTime.of(2026, 7, 1, 0, 0),
                java.time.LocalDateTime.of(2026, 7, 20, 0, 0),
                true,
                List.of(new TmdbFieldDiffDto("title", "Tên phim", "Old", "New", true)),
                List.of(new TmdbCollectionDiffDto("genres", "Thể loại", List.of("Drama"),
                        List.of("Action"), List.of("Action"), List.of("Drama"), true)));
        when(tmdbMovieReviewService.getReview("movie-1")).thenReturn(review);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/admin/movies/movie-1/tmdb-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.source").value("TMDB"))
                .andExpect(jsonPath("$.data.tmdbId").value(501))
                .andExpect(jsonPath("$.data.reviewStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.canApprove").value(true))
                .andExpect(jsonPath("$.data.readiness.healthStatus").value("READY"))
                .andExpect(jsonPath("$.data.hasProviderChanges").value(true))
                .andExpect(jsonPath("$.data.scalarDiffs[0].field").value("title"))
                .andExpect(jsonPath("$.data.collectionDiffs[0].added[0]").value("Action"));
    }

    @Test
    void getTmdbReview_MapsProviderFailureWithoutHidingDomainError() throws Exception {
        when(tmdbMovieReviewService.getReview("movie-1")).thenThrow(
                new com.lorafilm.movie.common.exception.BusinessException(
                        com.lorafilm.movie.common.exception.ErrorCode.TMDB_PROVIDER_UNAVAILABLE));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/admin/movies/movie-1/tmdb-review"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TMDB_PROVIDER_UNAVAILABLE"));
    }

    @Test
    void getMovies_BindsAdvancedFilterContract() throws Exception {
        com.lorafilm.movie.common.dto.PageResponse<MovieDto> pageResponse = new com.lorafilm.movie.common.dto.PageResponse<>(
                List.of(), 0, 20, 0, 0, true);
        when(movieService.getMovies(argThat(query ->
                "TMDB".equals(query.getSource())
                        && "WARNING".equals(query.getHealthStatus())
                        && "false".equals(query.getHasPrimaryPoster())
                        && "true".equals(query.getHasActiveVersion())
                        && "false".equals(query.getHasShowtime())
                        && "genre-public-id".equals(query.getGenrePublicId())
                        && "VN".equals(query.getCountry())
                        && LocalDate.of(2026, 1, 1).equals(query.getReleaseDateFrom())
                        && LocalDate.of(2026, 12, 31).equals(query.getReleaseDateTo())
                        && LocalDate.of(2026, 2, 1).equals(query.getTmdbUpdatedFrom())
                        && LocalDate.of(2026, 2, 28).equals(query.getTmdbUpdatedTo())
                        && query.getPage() == 0
                        && query.getSize() == 20
                        && "updatedAt,desc".equals(query.getSort())))).thenReturn(pageResponse);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("source", "TMDB")
                        .param("healthStatus", "WARNING")
                        .param("hasPrimaryPoster", "false")
                        .param("hasActiveVersion", "true")
                        .param("hasShowtime", "false")
                        .param("genrePublicId", "genre-public-id")
                        .param("country", "VN")
                        .param("releaseDateFrom", "2026-01-01")
                        .param("releaseDateTo", "2026-12-31")
                        .param("tmdbUpdatedFrom", "2026-02-01")
                        .param("tmdbUpdatedTo", "2026-02-28")
                        .param("size", "20")
                        .param("sort", "updatedAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void getMovies_InvalidPageAndDateReturnValidationErrors() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/admin/movies")
                        .param("releaseDateFrom", "22-07-2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
