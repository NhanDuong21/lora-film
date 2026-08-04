package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.dto.AdminMovieListQuery;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplQueryTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieGenreRepository movieGenreRepository;
    @Mock private MovieMediaRepository movieMediaRepository;
    @Mock private MovieCreditRepository movieCreditRepository;
    @Mock private MovieProductionCompanyRepository movieProductionCompanyRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private MovieMapper movieMapper;
    @Mock private MovieReadinessEvaluator readinessEvaluator;
    @Mock private AdminMovieProjectionService projectionService;
    @Mock private MovieLifecyclePolicy lifecyclePolicy;
    @Mock private MovieApprovalPolicy approvalPolicy;

    private MovieServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MovieServiceImpl(
                movieRepository,
                movieGenreRepository,
                movieMediaRepository,
                movieCreditRepository,
                movieProductionCompanyRepository,
                movieVersionRepository,
                movieMapper,
                readinessEvaluator,
                projectionService,
                lifecyclePolicy,
                approvalPolicy);
        Page<Movie> emptyPage = new PageImpl<>(List.of());
        lenient().when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);
        lenient().when(projectionService.enrichMovies(emptyPage)).thenReturn(new PageResponse<>());
    }

    @Test
    void defaultSortIsStableAndDeterministic() {
        service.getMovies(new AdminMovieListQuery());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(movieRepository).findAll(any(Specification.class), pageable.capture());
        List<Sort.Order> orders = pageable.getValue().getSort().stream().toList();
        assertEquals(2, orders.size());
        assertEquals("releaseDate", orders.get(0).getProperty());
        assertTrue(orders.get(0).isDescending());
        assertEquals("id", orders.get(1).getProperty());
        assertTrue(orders.get(1).isDescending());
    }

    @Test
    void acceptsCombinedAdvancedFiltersBeforePaging() {
        AdminMovieListQuery query = new AdminMovieListQuery();
        query.setStatus("draft");
        query.setKeyword("  Batman  ");
        query.setCity("  HCM  ");
        query.setCinemaId(2L);
        query.setDate(LocalDate.of(2026, 7, 22));
        query.setSource("tmdb");
        query.setHealthStatus("warning");
        query.setHasPrimaryPoster("false");
        query.setHasActiveVersion("true");
        query.setHasShowtime("false");
        query.setGenreId(1L);
        query.setGenrePublicId("genre-public-id");
        query.setCountry("  VN ");
        query.setReleaseDateFrom(LocalDate.of(2026, 1, 1));
        query.setReleaseDateTo(LocalDate.of(2026, 12, 31));
        query.setTmdbUpdatedFrom(LocalDate.of(2026, 2, 1));
        query.setTmdbUpdatedTo(LocalDate.of(2026, 2, 28));
        query.setSort("title,asc");
        query.setPage(2);
        query.setSize(20);

        service.getMovies(query);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(movieRepository).findAll(any(Specification.class), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals("title", pageable.getValue().getSort().stream().findFirst().orElseThrow().getProperty());
    }

    @Test
    void rejectsUnsupportedOrUnstableSortInputs() {
        assertValidationError(queryWithSort("id,asc"));
        assertValidationError(queryWithSort("title,sideways"));
        assertValidationError(queryWithSort("title"));
        assertValidationError(queryWithSort("title,asc,extra"));
        assertValidationError(queryWithSort("  "));
    }

    @Test
    void acceptsEveryWhitelistedSortWithStableIdTieBreaker() {
        List<String> fields = List.of("updatedAt", "releaseDate", "title", "tmdbLastUpdated", "createdAt");

        for (String field : fields) {
            for (String direction : List.of("asc", "desc")) {
                AdminMovieListQuery query = queryWithSort(field + "," + direction);
                service.getMovies(query);
            }
        }

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(movieRepository, times(10)).findAll(any(Specification.class), pageable.capture());
        for (Pageable captured : pageable.getAllValues()) {
            List<Sort.Order> orders = captured.getSort().stream().toList();
            assertEquals(2, orders.size());
            assertEquals("id", orders.get(1).getProperty());
            assertTrue(orders.get(1).isDescending());
        }
    }

    @Test
    void acceptsBothExactBooleanValuesForEveryBooleanFilter() {
        for (String value : List.of("true", "false")) {
            AdminMovieListQuery poster = new AdminMovieListQuery();
            poster.setHasPrimaryPoster(value);
            service.getMovies(poster);

            AdminMovieListQuery version = new AdminMovieListQuery();
            version.setHasActiveVersion(value);
            service.getMovies(version);

            AdminMovieListQuery showtime = new AdminMovieListQuery();
            showtime.setHasShowtime(value);
            service.getMovies(showtime);
        }

        verify(movieRepository, times(6)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void rejectsInvalidEnumsBooleansAndRanges() {
        AdminMovieListQuery status = new AdminMovieListQuery();
        status.setStatus("PUBLISHED");
        assertValidationError(status);

        AdminMovieListQuery source = new AdminMovieListQuery();
        source.setSource("IMPORT");
        assertValidationError(source);

        AdminMovieListQuery health = new AdminMovieListQuery();
        health.setHealthStatus("INCOMPLETE");
        assertValidationError(health);

        AdminMovieListQuery bool = new AdminMovieListQuery();
        bool.setHasShowtime("yes");
        assertValidationError(bool);

        AdminMovieListQuery uppercaseBool = new AdminMovieListQuery();
        uppercaseBool.setHasPrimaryPoster("TRUE");
        assertValidationError(uppercaseBool);

        AdminMovieListQuery paddedBool = new AdminMovieListQuery();
        paddedBool.setHasActiveVersion(" true ");
        assertValidationError(paddedBool);

        AdminMovieListQuery releaseRange = new AdminMovieListQuery();
        releaseRange.setReleaseDateFrom(LocalDate.of(2026, 2, 2));
        releaseRange.setReleaseDateTo(LocalDate.of(2026, 2, 1));
        assertValidationError(releaseRange);

        AdminMovieListQuery tmdbRange = new AdminMovieListQuery();
        tmdbRange.setTmdbUpdatedFrom(LocalDate.of(2026, 3, 2));
        tmdbRange.setTmdbUpdatedTo(LocalDate.of(2026, 3, 1));
        assertValidationError(tmdbRange);
    }

    @Test
    void acceptsAllAsTheLegacyUnfilteredLifecycleValue() {
        AdminMovieListQuery query = new AdminMovieListQuery();
        query.setStatus("all");

        service.getMovies(query);

        verify(movieRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private AdminMovieListQuery queryWithSort(String sort) {
        AdminMovieListQuery query = new AdminMovieListQuery();
        query.setSort(sort);
        return query;
    }

    private void assertValidationError(AdminMovieListQuery query) {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.getMovies(query));
        assertEquals("VALIDATION_ERROR", exception.getErrorCode().name());
    }
}
