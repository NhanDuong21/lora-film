package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.AnalyticsJobDomainService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsJobWorker {
    private final AnalyticsJobDomainService domainService;

    public AnalyticsJobWorker(AnalyticsJobDomainService domainService) {
        this.domainService = domainService;
    }

    @Async("analyticsJobExecutor")
    public void run(long jobId) {
        domainService.run(jobId);
    }
}
