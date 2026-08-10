package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieDetailsDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieReviewResponse;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieCreditRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieProductionCompanyRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieTranslationRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.service.MovieLifecyclePolicy;
import com.lorafilm.movie.movie.service.MovieApprovalPolicy;
import com.lorafilm.movie.movie.service.MovieReadinessEvaluator;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TmdbMovieReviewServiceTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieGenreRepository movieGenreRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private MovieMediaRepository movieMediaRepository;
    @Mock private MovieCreditRepository movieCreditRepository;
    @Mock private MovieProductionCompanyRepository movieProductionCompanyRepository;
    @Mock private MovieTranslationRepository movieTranslationRepository;
    @Mock private TmdbProviderMovieService providerMovieService;
    @Mock private TmdbMovieMapper movieMapper;
    @Mock private ShowtimeRepository showtimeRepository;

    private TmdbMovieReviewService service;

    @BeforeEach
    void setUp() {
        MovieLifecyclePolicy lifecyclePolicy = new MovieLifecyclePolicy();
        service = new TmdbMovieReviewService(
                movieRepository, movieGenreRepository, movieVersionRepository, movieMediaRepository,
                movieCreditRepository, movieProductionCompanyRepository, movieTranslationRepository,
                providerMovieService, movieMapper, new MovieReadinessEvaluator(),
                new MovieApprovalPolicy(lifecyclePolicy, showtimeRepository));
    }

    @Test
    void readyDraftTmdbMovieCanBeApprovedAndProducesLiveDiff() {
        Movie movie = movie(1L, 501L, MovieStatus.DRAFT);
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        TmdbMovieWrapperDto provider = provider(movie);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-1")).thenReturn(Optional.of(movie));
        when(providerMovieService.fetchMovie(501L)).thenReturn(provider);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(genreLink(movie, "Action")));
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        stubEmptyAggregate(1L);
        when(movieMapper.extractTitle(provider)).thenReturn(movie.getTitle());
        when(movieMapper.extractOverview(provider)).thenReturn(movie.getSynopsis());
        when(movieMapper.extractReleaseDate(provider)).thenReturn(movie.getReleaseDate());
        when(movieMapper.extractCountry(provider)).thenReturn(movie.getCountry());

        TmdbMovieReviewResponse review = service.getReview("movie-1");

        assertEquals("PENDING", review.reviewStatus());
        assertEquals(MovieHealthStatus.READY, review.readiness().getHealthStatus());
        assertTrue(review.canApprove());
        assertEquals(MovieStatus.UPCOMING, review.approvalTarget());
        assertTrue(review.approvalBlockers().isEmpty());
        assertTrue(review.scalarDiffs().stream().noneMatch(diff -> diff.changed()));
    }

    @Test
    void canonicalReadinessBlockersPreventApproval() {
        Movie movie = movie(2L, 502L, MovieStatus.DRAFT);
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        TmdbMovieWrapperDto provider = provider(movie);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-2")).thenReturn(Optional.of(movie));
        when(providerMovieService.fetchMovie(502L)).thenReturn(provider);
        when(movieGenreRepository.findByMovieId(2L)).thenReturn(List.of());
        when(movieVersionRepository.existsActiveVersion(2L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(2L)).thenReturn(false);
        stubEmptyAggregate(2L);
        when(movieMapper.extractTitle(provider)).thenReturn(movie.getTitle());
        when(movieMapper.extractOverview(provider)).thenReturn(movie.getSynopsis());
        when(movieMapper.extractReleaseDate(provider)).thenReturn(movie.getReleaseDate());
        when(movieMapper.extractCountry(provider)).thenReturn(movie.getCountry());

        TmdbMovieReviewResponse review = service.getReview("movie-2");

        assertEquals(MovieHealthStatus.BLOCKED, review.readiness().getHealthStatus());
        assertFalse(review.canApprove());
        assertEquals(3, review.approvalBlockers().size());
    }

    @Test
    void manualMovieDoesNotCallProvider() {
        Movie manual = movie(3L, null, MovieStatus.DRAFT);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-3")).thenReturn(Optional.of(manual));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getReview("movie-3"));

        assertEquals(ErrorCode.TMDB_MOVIE_REVIEW_NOT_APPLICABLE, exception.getErrorCode());
        verifyNoInteractions(providerMovieService);
    }

    private void stubEmptyAggregate(Long movieId) {
        when(movieMediaRepository.findByMovieIdAndDeletedAtIsNull(movieId)).thenReturn(List.of());
        when(movieCreditRepository.findByMovieIdAndDeletedAtIsNullOrderByDisplayOrderAsc(movieId)).thenReturn(List.of());
        when(movieProductionCompanyRepository.findByMovieId(movieId)).thenReturn(List.of());
        when(movieTranslationRepository.findByMovieId(movieId)).thenReturn(List.of());
    }

    private Movie movie(Long id, Long tmdbId, MovieStatus status) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setPublicId("movie-" + id);
        movie.setTmdbId(tmdbId);
        movie.setTmdbLastUpdated(LocalDateTime.of(2026, 7, 1, 0, 0));
        movie.setStatus(status);
        movie.setTitle("Movie " + id);
        movie.setOriginalTitle("Movie " + id);
        movie.setSynopsis("Synopsis");
        movie.setDurationMinutes(120);
        movie.setAgeRating(AgeRating.P);
        movie.setReleaseDate(LocalDate.now().plusDays(1));
        movie.setCountry("VN");
        return movie;
    }

    private TmdbMovieWrapperDto provider(Movie movie) {
        TmdbMovieDetailsDto details = new TmdbMovieDetailsDto();
        details.setTmdbId(movie.getTmdbId());
        details.setTitle(movie.getTitle());
        details.setOriginalTitle(movie.getOriginalTitle());
        details.setOverview(movie.getSynopsis());
        details.setRuntimeMinutes(movie.getDurationMinutes());
        details.setAdult(false);
        TmdbMovieWrapperDto wrapper = new TmdbMovieWrapperDto();
        wrapper.setTmdbId(movie.getTmdbId());
        wrapper.setLastUpdated(LocalDateTime.of(2026, 7, 20, 0, 0));
        wrapper.setMovie(details);
        return wrapper;
    }

    private MovieGenre genreLink(Movie movie, String name) {
        Genre genre = new Genre();
        genre.setName(name);
        MovieGenre link = new MovieGenre();
        link.setMovie(movie);
        link.setGenre(genre);
        return link;
    }
}
