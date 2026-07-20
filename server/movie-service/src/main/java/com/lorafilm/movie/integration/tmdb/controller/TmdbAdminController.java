package com.lorafilm.movie.integration.tmdb.controller;

import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tmdb")
public class TmdbAdminController {

    private static final Logger log = LoggerFactory.getLogger(TmdbAdminController.class);

    private final TmdbImportService tmdbImportService;

    public TmdbAdminController(TmdbImportService tmdbImportService) {
        this.tmdbImportService = tmdbImportService;
    }

    @PostMapping("/sync/{tmdbId}")
    public ResponseEntity<String> syncMovieById(@PathVariable Long tmdbId) {
        log.info("[TmdbAdminController] Request to sync movie by ID: {}", tmdbId);
        try {
            tmdbImportService.importMovieById(tmdbId);
            log.info("[TmdbAdminController] Successfully synced movie ID: {}", tmdbId);
            return ResponseEntity.ok("Movie synced successfully");
        } catch (Exception e) {
            log.error("[TmdbAdminController] Failed to sync movie ID: {}", tmdbId, e);
            return ResponseEntity.badRequest().body("Failed to sync movie: " + e.getMessage());
        }
    }

    @PostMapping("/sync/bulk/start")
    public ResponseEntity<String> startBulkSync() {
        log.info("[TmdbAdminController] Request to start bulk export sync");
        new Thread(() -> {
            try {
                tmdbImportService.runBulkSync();
            } catch (Exception e) {
                log.error("[TmdbAdminController] Error running bulk sync thread", e);
            }
        }).start();
        return ResponseEntity.ok("Bulk sync started in the background");
    }

    @PostMapping("/sync/bulk/reset")
    public ResponseEntity<String> resetBulkSync() {
        log.info("[TmdbAdminController] Request to reset bulk export sync cursor and start background thread");
        new Thread(() -> {
            try {
                tmdbImportService.runBulkSync(true);
            } catch (Exception e) {
                log.error("[TmdbAdminController] Error running reset bulk sync thread", e);
            }
        }).start();
        return ResponseEntity.ok("Bulk sync reset to cursor 0 and started in the background");
    }
}
