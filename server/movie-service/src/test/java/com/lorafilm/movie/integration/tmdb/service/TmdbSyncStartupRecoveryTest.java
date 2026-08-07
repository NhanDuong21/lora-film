package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TmdbSyncStartupRecoveryTest {

    private final TmdbSyncStateRepository repository = mock(TmdbSyncStateRepository.class);
    private final TmdbSyncStartupRecovery recovery = new TmdbSyncStartupRecovery(repository);

    @Test
    void recoversInterruptedStateWhenMovieServiceStarts() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("IN_PROGRESS");
        when(repository.findBySyncType("TMDB_BULK_EXPORT")).thenReturn(Optional.of(state));

        recovery.run(null);

        assertEquals("IDLE", state.getStatus());
        assertTrue(state.getStatusMessage().contains("Movie Service khởi động lại"));
        verify(repository).save(state);
    }

    @Test
    void keepsCompletedStateUnchanged() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("COMPLETED");
        when(repository.findBySyncType("TMDB_BULK_EXPORT")).thenReturn(Optional.of(state));

        recovery.run(null);

        assertEquals("COMPLETED", state.getStatus());
        verify(repository, never()).save(state);
    }
}
