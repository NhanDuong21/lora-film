package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.AnalyticsJobDomainService;
import com.project.analyticsservice.dto.AnalyticsCommands;
import com.project.analyticsservice.dto.AnalyticsResponses;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsJobApplicationService {
    private final AnalyticsJobDomainService domainService;
    private final AnalyticsJobWorker worker;

    public AnalyticsJobApplicationService(
            AnalyticsJobDomainService domainService,
            AnalyticsJobWorker worker) {
        this.domainService = domainService;
        this.worker = worker;
    }

    public AnalyticsResponses.Job submit(
            AnalyticsCommands.RebuildJob request, String requestedBy) {
        AnalyticsResponses.Job job = domainService.submit(request, requestedBy);
        if ("QUEUED".equals(job.status())) {
            worker.run(job.id());
        }
        return job;
    }

    public List<AnalyticsResponses.Job> recent() {
        return domainService.recent();
    }
}
