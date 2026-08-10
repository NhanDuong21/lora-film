package com.project.userservice.scheduler;

import com.project.userservice.service.PiiGovernanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PiiRetentionScheduler {
    private final PiiGovernanceService service;

    public PiiRetentionScheduler(PiiGovernanceService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.scheduler.pii-retention.cron:0 30 2 * * ?}")
    public void eraseExpiredPii() {
        service.anonymizeExpiredRecords();
    }
}
