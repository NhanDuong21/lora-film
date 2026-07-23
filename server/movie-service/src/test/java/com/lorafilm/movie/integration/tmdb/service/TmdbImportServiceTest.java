package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImportOutcome;
import com.lorafilm.movie.integration.tmdb.dto.TmdbImportResult;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieDetailsDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TmdbImportServiceTest {

    @Mock private TmdbClient tmdbClient;
    @Mock private TmdbProperties properties;
    @Mock private MovieRepository movieRepository;
    @Mock private TmdbMovieMapper movieMapper;
    @Mock private TmdbSyncStateRepository syncStateRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private GenreRepository genreRepository;
    @Mock private MovieGenreRepository movieGenreRepository;
    @Mock private PersonRepository personRepository;
    @Mock private MovieCreditRepository movieCreditRepository;
    @Mock private MovieMediaRepository movieMediaRepository;
    @Mock private MovieTranslationRepository movieTranslationRepository;
    @Mock private ProductionCompanyRepository productionCompanyRepository;
    @Mock private MovieProductionCompanyRepository movieProductionCompanyRepository;
    @Mock private MovieVersionRepository movieVersionRepository;
    @Mock private TmdbProviderMovieService providerMovieService;

    private TmdbImportService importService;

    @BeforeEach
    void setUp() {
        importService = new TmdbImportService(
                tmdbClient, properties, movieRepository, movieMapper, syncStateRepository, objectMapper,
                genreRepository, movieGenreRepository, personRepository, movieCreditRepository,
                movieMediaRepository, movieTranslationRepository, productionCompanyRepository,
                movieProductionCompanyRepository, movieVersionRepository, providerMovieService
        );
    }

    @Test
    void testStopBulkSync_InterruptsThread() throws Exception {
        TmdbSyncState state = new TmdbSyncState();
        state.setSyncType("TMDB_BULK_EXPORT");
        state.setStatus("IDLE");
        state.setCursor("0");
        
        when(properties.isSyncEnabled()).thenReturn(true);
        when(syncStateRepository.findBySyncType("TMDB_BULK_EXPORT")).thenReturn(Optional.of(state));
        when(syncStateRepository.save(any(TmdbSyncState.class))).thenReturn(state);
        when(properties.getBatchSize()).thenReturn(100);
        
        // When the service tries to fetch from TMDB, we simulate a stop request and then throw an error
        when(tmdbClient.fetchMoviesExport(any(), anyInt())).thenAnswer(invocation -> {
            importService.stopBulkSync();
            throw new RuntimeException("Simulated error");
        });
        
        // Execute
        importService.runBulkSync(false);
        
        // Verify that it ended in IDLE state because we requested a stop during fetch
        ArgumentCaptor<TmdbSyncState> stateCaptor = ArgumentCaptor.forClass(TmdbSyncState.class);
        verify(syncStateRepository, atLeastOnce()).save(stateCaptor.capture());
        
        assertEquals("IDLE", stateCaptor.getValue().getStatus());
    }

    @Test
    void autoApprovedMovieIsAlwaysCreatedAsDraft() {
        TmdbMovieWrapperDto wrapper = wrapper(101L, "AUTO_APPROVED");
        Movie mappedMovie = movie(10L, 101L, MovieStatus.UPCOMING);
        when(movieRepository.findByTmdbId(101L)).thenReturn(Optional.empty());
        when(movieMapper.toEntity(wrapper)).thenReturn(mappedMovie);
        when(movieMapper.generateSlug("Provider movie")).thenReturn("provider-movie");
        when(movieRepository.findBySlugAndDeletedAtIsNull("provider-movie")).thenReturn(Optional.empty());
        when(movieRepository.saveAndFlush(mappedMovie)).thenReturn(mappedMovie);
        when(movieVersionRepository.findByMovieIdAndDeletedAtIsNull(10L)).thenReturn(java.util.List.of());

        TmdbImportResult result = importService.importMovie(wrapper);

        assertEquals(TmdbImportOutcome.CREATED, result.outcome());
        assertEquals(MovieStatus.DRAFT, mappedMovie.getStatus());
        verify(movieRepository).saveAndFlush(mappedMovie);
        verify(movieMapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    void existingTmdbMoviePreservesLifecycleAndAllRelations() {
        TmdbMovieWrapperDto wrapper = wrapper(102L, "AUTO_APPROVED");
        Movie existing = movie(11L, 102L, MovieStatus.NOW_SHOWING);
        existing.setTitle("Admin curated title");
        when(movieRepository.findByTmdbId(102L)).thenReturn(Optional.of(existing));

        TmdbImportResult result = importService.importMovie(wrapper);

        assertEquals(TmdbImportOutcome.ALREADY_IMPORTED, result.outcome());
        assertEquals(MovieStatus.NOW_SHOWING, existing.getStatus());
        assertEquals("Admin curated title", existing.getTitle());
        verify(movieRepository, never()).save(any());
        verify(movieMapper, never()).updateEntityFromDto(any(), any());
        verifyNoInteractions(movieGenreRepository, movieCreditRepository, movieMediaRepository,
                movieTranslationRepository, movieProductionCompanyRepository, movieVersionRepository);
    }

    @Test
    void softDeletedTmdbMovieRemainsATombstone() {
        TmdbMovieWrapperDto wrapper = wrapper(103L, "ACCEPT");
        Movie deleted = movie(12L, 103L, MovieStatus.INACTIVE);
        deleted.setDeletedAt(Instant.parse("2026-01-01T00:00:00Z"));
        when(movieRepository.findByTmdbId(103L)).thenReturn(Optional.of(deleted));

        TmdbImportResult result = importService.importMovie(wrapper);

        assertEquals(TmdbImportOutcome.DELETED_TOMBSTONE, result.outcome());
        verify(movieRepository, never()).save(any());
    }

    @Test
    void providerRejectedMovieIsNotPersisted() {
        TmdbImportResult result = importService.importMovie(wrapper(104L, "REJECTED"));

        assertEquals(TmdbImportOutcome.REJECTED_BY_PROVIDER, result.outcome());
        verifyNoInteractions(movieRepository);
    }

    @Test
    void slugCollisionNeverSelectsOrMutatesAManualMovie() {
        TmdbMovieWrapperDto wrapper = wrapper(105L, "ACCEPT");
        Movie mappedMovie = movie(13L, 105L, MovieStatus.DRAFT);
        Movie manual = movie(14L, null, MovieStatus.DRAFT);
        when(movieRepository.findByTmdbId(105L)).thenReturn(Optional.empty());
        when(movieMapper.toEntity(wrapper)).thenReturn(mappedMovie);
        when(movieMapper.generateSlug("Provider movie")).thenReturn("provider-movie");
        when(movieRepository.findBySlugAndDeletedAtIsNull("provider-movie")).thenReturn(Optional.of(manual));
        when(movieRepository.saveAndFlush(mappedMovie)).thenReturn(mappedMovie);
        when(movieVersionRepository.findByMovieIdAndDeletedAtIsNull(13L)).thenReturn(java.util.List.of());

        importService.importMovie(wrapper);

        assertEquals(105L, mappedMovie.getTmdbId());
        assertEquals("provider-movie-105", mappedMovie.getSlug());
        verify(movieMapper, never()).updateEntityFromDto(any(), eq(manual));
    }

    @Test
    void concurrentInsertLosingUniqueKeyRaceReturnsExistingOutcome() {
        TmdbMovieWrapperDto wrapper = wrapper(106L, "ACCEPT");
        Movie candidate = movie(15L, 106L, MovieStatus.DRAFT);
        Movie winner = movie(16L, 106L, MovieStatus.DRAFT);
        when(movieRepository.findByTmdbId(106L)).thenReturn(Optional.empty(), Optional.of(winner));
        when(movieMapper.toEntity(wrapper)).thenReturn(candidate);
        when(movieMapper.generateSlug("Provider movie")).thenReturn("provider-movie");
        when(movieRepository.findBySlugAndDeletedAtIsNull("provider-movie")).thenReturn(Optional.empty());
        when(movieRepository.saveAndFlush(candidate)).thenThrow(new DataIntegrityViolationException("duplicate tmdb_id"));

        TmdbImportResult result = importService.importMovieSafely(wrapper);

        assertEquals(TmdbImportOutcome.ALREADY_IMPORTED, result.outcome());
        assertEquals(winner.getPublicId(), result.moviePublicId());
    }

    private TmdbMovieWrapperDto wrapper(Long tmdbId, String status) {
        TmdbMovieDetailsDto details = new TmdbMovieDetailsDto();
        details.setTmdbId(tmdbId);
        details.setTitle("Provider movie");
        TmdbMovieWrapperDto wrapper = new TmdbMovieWrapperDto();
        wrapper.setTmdbId(tmdbId);
        wrapper.setApprovalStatus(status);
        wrapper.setMovie(details);
        return wrapper;
    }

    private Movie movie(Long id, Long tmdbId, MovieStatus status) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setPublicId("movie-" + id);
        movie.setTmdbId(tmdbId);
        movie.setTitle("Provider movie");
        movie.setStatus(status);
        return movie;
    }
}
