package com.project.scoreservice.scheduler;

import com.project.scoreservice.service.ScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PointExpirationScheduler {
    private static final Logger log = LoggerFactory.getLogger(PointExpirationScheduler.class);

    private final ScoreService scoreService;

    public PointExpirationScheduler(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduleExpiration() {
        log.info("Starting daily point expiration job...");
        try {
            scoreService.expirePoints();
            log.info("Completed daily point expiration job.");
        } catch (Exception e) {
            log.error("Error occurred during point expiration job", e);
        }
    }
}
