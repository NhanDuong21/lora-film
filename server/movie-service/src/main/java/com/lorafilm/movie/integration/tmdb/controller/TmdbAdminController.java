package com.lorafilm.movie.integration.tmdb.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lorafilm.movie.common.api.ApiResponse;
import com.lorafilm.movie.integration.tmdb.domain.entity.TmdbSyncState;
import com.lorafilm.movie.integration.tmdb.dto.TmdbSyncStateDto;
import com.lorafilm.movie.integration.tmdb.repository.TmdbSyncStateRepository;
import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;

@RestController
@RequestMapping("/api/admin/tmdb")
public class TmdbAdminController {

    private static final Logger log = LoggerFactory.getLogger(TmdbAdminController.class);

    private final TmdbImportService tmdbImportService;
    private final TmdbSyncStateRepository syncStateRepository;

    public TmdbAdminController(TmdbImportService tmdbImportService, TmdbSyncStateRepository syncStateRepository) {
        this.tmdbImportService = tmdbImportService;
        this.syncStateRepository = syncStateRepository;
    }

    @GetMapping("/sync/state")
    public ResponseEntity<ApiResponse<TmdbSyncStateDto>> getSyncState() {
        log.info("[TmdbAdminController] Request to get sync state");
        Optional<TmdbSyncState> stateOpt = syncStateRepository.findBySyncType("DAILY_CHANGES");

        TmdbSyncStateDto dto = new TmdbSyncStateDto();
        if (stateOpt.isPresent()) {
            TmdbSyncState state = stateOpt.get();
            dto.setStatus(state.getStatus());
            dto.setCursor(state.getCursor());
            dto.setLastCompletedAt(state.getLastSyncTime());
            // Other fields like startedAt, lastSuccessAt, lastFailureAt, lastError are NOT persisted in the database.
            // Requirement strictly forbids faking these metrics.
        } else {
            dto.setStatus("IDLE");
        }

        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/sync/{tmdbId}")
    public ResponseEntity<ApiResponse<String>> syncMovieById(@PathVariable Long tmdbId) {
        log.info("[TmdbAdminController] Request to sync movie by ID: {}", tmdbId);
        try {
            tmdbImportService.importMovieById(tmdbId);
            log.info("[TmdbAdminController] Successfully synced movie ID: {}", tmdbId);
            return ResponseEntity.ok(ApiResponse.ok("Movie synced successfully"));
        } catch (Exception e) {
            log.error("[TmdbAdminController] Failed to sync movie ID: {}", tmdbId, e);
            return ResponseEntity.badRequest().body(ApiResponse.fail("SYNC_ERROR", "Failed to sync movie: " + e.getMessage()));
        }
    }

    @PostMapping("/sync/bulk/start")
    public ResponseEntity<ApiResponse<String>> startBulkSync() {
        log.info("[TmdbAdminController] Request to start bulk export sync");
        new Thread(() -> {
            try {
                tmdbImportService.runBulkSync();
            } catch (Exception e) {
                log.error("[TmdbAdminController] Error running bulk sync thread", e);
            }
        }).start();
        return ResponseEntity.ok(ApiResponse.ok("Bulk sync started in the background"));
    }

    @PostMapping("/sync/bulk/stop")
    public ResponseEntity<String> stopBulkSync() {
        log.info("[TmdbAdminController] Request to stop bulk export sync");
        tmdbImportService.stopBulkSync();
        return ResponseEntity.ok("Bulk sync stop signal sent");
    }

    @GetMapping("/sync/bulk/status")
    public ResponseEntity<TmdbSyncState> getBulkSyncStatus() {
        return ResponseEntity.ok(tmdbImportService.getBulkSyncStatus());
    }

    @PostMapping("/sync/bulk/reset")
    public ResponseEntity<ApiResponse<String>> resetBulkSync() {
        log.info("[TmdbAdminController] Request to reset bulk export sync cursor and start background thread");
        new Thread(() -> {
            try {
                tmdbImportService.runBulkSync(true);
            } catch (Exception e) {
                log.error("[TmdbAdminController] Error running reset bulk sync thread", e);
            }
        }).start();
        return ResponseEntity.ok(ApiResponse.ok("Bulk sync reset to cursor 0 and started in the background"));
    }
}
