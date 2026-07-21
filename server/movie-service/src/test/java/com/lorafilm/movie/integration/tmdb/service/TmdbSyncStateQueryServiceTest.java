package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.dto.TmdbSyncStateDto;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TmdbSyncStateQueryServiceTest {

    @Mock
    private TmdbSyncStateRepository syncStateRepository;

    @Mock
    private TmdbProperties tmdbProperties;

    @InjectMocks
    private TmdbSyncStateQueryService queryService;

    @BeforeEach
    void setUp() {
        lenient().when(tmdbProperties.getSyncStaleThresholdSeconds()).thenReturn(300);
    }

    @Test
    void testGetSyncState_NoData() {
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.empty());

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("NO_DATA", result.getDisplayStatus());
        assertNull(result.getPersistedStatus());
        assertFalse(result.isStale());
    }

    @Test
    void testGetSyncState_Idle() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("IDLE");
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.of(state));

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("IDLE", result.getDisplayStatus());
        assertEquals("IDLE", result.getPersistedStatus());
        assertFalse(result.isStale());
    }

    @Test
    void testGetSyncState_Running_NotStale() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("IN_PROGRESS");
        state.setUpdatedAt(LocalDateTime.now().minusSeconds(100)); // less than 300
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.of(state));

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("RUNNING", result.getDisplayStatus());
        assertFalse(result.isStale());
    }

    @Test
    void testGetSyncState_Running_Stale() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("IN_PROGRESS");
        state.setUpdatedAt(LocalDateTime.now().minusSeconds(400)); // more than 300
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.of(state));

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("STALE", result.getDisplayStatus());
        assertTrue(result.isStale());
    }

    @Test
    void testGetSyncState_Success() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("COMPLETED");
        LocalDateTime lastSync = LocalDateTime.now();
        state.setLastSyncTime(lastSync);
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.of(state));

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("SUCCESS", result.getDisplayStatus());
        assertEquals(lastSync, result.getLastSuccessfulSyncAt());
    }

    @Test
    void testGetSyncState_Failed() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("FAILED");
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.of(state));

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("FAILED", result.getDisplayStatus());
    }

    @Test
    void testGetSyncState_Unknown() {
        TmdbSyncState state = new TmdbSyncState();
        state.setStatus("WEIRD_STATUS");
        when(syncStateRepository.findBySyncType("TEST_SYNC")).thenReturn(Optional.of(state));

        TmdbSyncStateDto result = queryService.getSyncState("TEST_SYNC");

        assertEquals("UNKNOWN", result.getDisplayStatus());
    }
}
