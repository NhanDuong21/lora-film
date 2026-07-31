package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.AnalyticsLifecycleDomainService;
import com.project.analyticsservice.dto.AnalyticsResponses;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsLifecycleApplicationService {
    private final AnalyticsLifecycleDomainService domainService;

    public AnalyticsLifecycleApplicationService(AnalyticsLifecycleDomainService domainService) {
        this.domainService = domainService;
    }

    public AnalyticsResponses.ActionResult acknowledgeAlert(long id, String actor) {
        return domainService.acknowledgeAlert(id, actor);
    }

    public AnalyticsResponses.ActionResult updateRecommendation(
            long id, String status, String actor) {
        return domainService.updateRecommendation(id, status, actor);
    }
}
