package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.AnalyticsQueryDomainService;
import com.project.analyticsservice.dto.AnalyticsResponses;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsQueryApplicationService {
    private final AnalyticsQueryDomainService domainService;

    public AnalyticsQueryApplicationService(AnalyticsQueryDomainService domainService) {
        this.domainService = domainService;
    }

    public AnalyticsResponses.Dashboard dashboard(
            String startDate, String endDate, String cinemaKey) {
        return domainService.dashboard(startDate, endDate, cinemaKey);
    }

    public List<AnalyticsResponses.CinemaKpi> cinemas(
            String startDate, String endDate, Integer limit) {
        return domainService.cinemas(startDate, endDate, limit);
    }
}
