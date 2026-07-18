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

    // Runs bulk export continuously with a delay (e.g., every 1 hour) to slowly sync the whole catalog
    @Scheduled(fixedDelayString = "${tmdb.scheduler.bulk-interval:3600000}")
    public void scheduleBulkSync() {
        log.info("Starting automated background TMDB Bulk Sync...");
        tmdbImportService.runBulkSync();
    }

    // Cron job for latest movies - runs every day at 01:00 AM
    @Scheduled(cron = "${tmdb.scheduler.latest-cron:0 0 1 * * ?}")
    public void scheduleDailyLatestSync() {
        log.info("Starting daily latest TMDB movie sync...");
        tmdbImportService.runDailyLatestSync();
        log.info("Daily latest TMDB movie sync completed.");
    }

    // Cron job for updated movies - runs every day at 02:00 AM
    @Scheduled(cron = "${tmdb.scheduler.updated-cron:0 0 2 * * ?}")
    public void scheduleDailyUpdatedSync() {
        log.info("Starting daily updated TMDB movie sync...");
        tmdbImportService.runDailyUpdatedSync();
        log.info("Daily updated TMDB movie sync completed.");
    }
}
