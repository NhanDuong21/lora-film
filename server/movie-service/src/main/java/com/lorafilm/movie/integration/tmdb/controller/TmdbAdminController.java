package com.lorafilm.movie.integration.tmdb.controller;

import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tmdb")
public class TmdbAdminController {

    private final TmdbImportService tmdbImportService;

    public TmdbAdminController(TmdbImportService tmdbImportService) {
        this.tmdbImportService = tmdbImportService;
    }

    @PostMapping("/sync/{tmdbId}")
    public ResponseEntity<String> syncMovieById(@PathVariable Long tmdbId) {
        try {
            tmdbImportService.importMovieById(tmdbId);
            return ResponseEntity.ok("Movie synced successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to sync movie: " + e.getMessage());
        }
    }
    
    @PostMapping("/sync/bulk/start")
    public ResponseEntity<String> startBulkSync() {
        new Thread(tmdbImportService::runBulkSync).start();
        return ResponseEntity.ok("Bulk sync started in the background");
    }
}
