package com.lorafilm.movie.integration.tmdb.service;

import com.lorafilm.movie.integration.tmdb.dto.TmdbBulkSyncRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class TmdbSyncJobLauncher {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncJobLauncher.class);
    private final TmdbImportService tmdbImportService;

    public TmdbSyncJobLauncher(TmdbImportService tmdbImportService) {
        this.tmdbImportService = tmdbImportService;
    }

    @Async("tmdbSyncExecutor")
    public void start(TmdbBulkSyncRequest request, boolean resetCursor) {
        try {
            tmdbImportService.runBulkSync(request, resetCursor);
        } catch (RuntimeException exception) {
            log.error("Không thể khởi chạy tiến trình nhập phim TMDB", exception);
        }
    }
}
