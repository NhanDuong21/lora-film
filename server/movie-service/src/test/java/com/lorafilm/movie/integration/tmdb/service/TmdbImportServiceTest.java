package com.lorafilm.movie.integration.tmdb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.integration.tmdb.client.TmdbClient;
import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.mapper.TmdbMovieMapper;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.movie.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    private TmdbImportService importService;

    @BeforeEach
    void setUp() {
        importService = new TmdbImportService(
                tmdbClient, properties, movieRepository, movieMapper, syncStateRepository, objectMapper,
                genreRepository, movieGenreRepository, personRepository, movieCreditRepository,
                movieMediaRepository, movieTranslationRepository, productionCompanyRepository,
                movieProductionCompanyRepository, movieVersionRepository
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
}
