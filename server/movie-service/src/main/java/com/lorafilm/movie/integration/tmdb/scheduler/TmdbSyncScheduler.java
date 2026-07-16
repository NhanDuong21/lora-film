package com.lorafilm.movie.integration.tmdb.scheduler;

import com.lorafilm.movie.integration.tmdb.service.TmdbImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class TmdbSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TmdbSyncScheduler.class);
    private final TmdbImportService tmdbImportService;

    public TmdbSyncScheduler(TmdbImportService tmdbImportService) {
        this.tmdbImportService = tmdbImportService;
    }

    // Runs every hour by default (3600000 ms), configurable via properties if needed
    @Scheduled(fixedDelayString = "${tmdb.scheduler-interval:3600000}")
    public void scheduleMovieSync() {
        log.info("Starting scheduled TMDB movie sync...");
        tmdbImportService.runSync();
        log.info("Scheduled TMDB movie sync completed.");
    }
}
