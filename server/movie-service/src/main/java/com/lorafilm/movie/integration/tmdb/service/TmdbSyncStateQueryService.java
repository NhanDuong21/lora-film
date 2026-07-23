package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.integration.tmdb.config.TmdbProperties;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.dto.TmdbSyncStateDto;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TmdbSyncStateQueryService {

    private final TmdbSyncStateRepository syncStateRepository;
    private final TmdbProperties tmdbProperties;

    public TmdbSyncStateQueryService(TmdbSyncStateRepository syncStateRepository, TmdbProperties tmdbProperties) {
        this.syncStateRepository = syncStateRepository;
        this.tmdbProperties = tmdbProperties;
    }

    public TmdbSyncStateDto getSyncState(String syncType) {
        Optional<TmdbSyncState> stateOpt = syncStateRepository.findBySyncType(syncType);
        TmdbSyncStateDto dto = new TmdbSyncStateDto();
        dto.setSyncType(syncType);
        dto.setStaleThresholdSeconds(tmdbProperties.getSyncStaleThresholdSeconds());

        if (stateOpt.isEmpty()) {
            dto.setPersistedStatus(null);
            dto.setDisplayStatus("NO_DATA");
            dto.setCursor(null);
            dto.setLastSuccessfulSyncAt(null);
            dto.setStateUpdatedAt(null);
            dto.setStale(false);
            return dto;
        }

        TmdbSyncState state = stateOpt.get();
        dto.setPersistedStatus(state.getStatus());
        dto.setCursor(state.getCursor());
        dto.setLastSuccessfulSyncAt(state.getLastSyncTime());
        dto.setStateUpdatedAt(state.getUpdatedAt());

        boolean isStale = false;
        if ("IN_PROGRESS".equals(state.getStatus()) && state.getUpdatedAt() != null) {
            LocalDateTime thresholdTime = LocalDateTime.now().minusSeconds(tmdbProperties.getSyncStaleThresholdSeconds());
            if (state.getUpdatedAt().isBefore(thresholdTime)) {
                isStale = true;
            }
        }
        dto.setStale(isStale);

        String displayStatus = deriveDisplayStatus(state.getStatus(), isStale);
        dto.setDisplayStatus(displayStatus);

        return dto;
    }

    private String deriveDisplayStatus(String persistedStatus, boolean isStale) {
        if (persistedStatus == null) {
            return "UNKNOWN";
        }
        
        switch (persistedStatus) {
            case "IDLE":
                return "IDLE";
            case "IN_PROGRESS":
                return isStale ? "STALE" : "RUNNING";
            case "COMPLETED":
                return "SUCCESS";
            case "FAILED":
                return "FAILED";
            default:
                return "UNKNOWN";
        }
    }
}
