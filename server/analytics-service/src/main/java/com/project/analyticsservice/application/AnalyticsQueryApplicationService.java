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

    public AnalyticsResponses.Dashboard dashboard(String startDate, String endDate) {
        return domainService.dashboard(startDate, endDate);
    }

    public List<AnalyticsResponses.DailyKpi> daily(String startDate, String endDate) {
        return domainService.daily(startDate, endDate);
    }

    public List<AnalyticsResponses.CinemaKpi> cinemas(
            String startDate, String endDate, Integer limit) {
        return domainService.cinemas(startDate, endDate, limit);
    }

    public List<AnalyticsResponses.MovieKpi> movies(
            String startDate, String endDate, Integer limit) {
        return domainService.movies(startDate, endDate, limit);
    }

    public List<AnalyticsResponses.PromotionKpi> promotions(
            String startDate, String endDate, Integer limit) {
        return domainService.promotions(startDate, endDate, limit);
    }

    public List<AnalyticsResponses.CustomerSegment> customerSegments(String date) {
        return domainService.customerSegments(date);
    }

    public List<AnalyticsResponses.Forecast> forecasts(String startDate, String endDate) {
        return domainService.forecasts(startDate, endDate);
    }

    public List<AnalyticsResponses.Insight> insights(String startDate, String endDate) {
        return domainService.insights(startDate, endDate);
    }

    public List<AnalyticsResponses.Recommendation> recommendations() {
        return domainService.recommendations();
    }

    public List<AnalyticsResponses.Alert> alerts() {
        return domainService.alerts();
    }

    public AnalyticsResponses.DataQuality dataQuality() {
        return domainService.dataQuality();
    }

    public AnalyticsResponses.HealthScore healthScore(String date) {
        return domainService.healthScore(date);
    }

    public List<AnalyticsResponses.Anomaly> anomalies(String startDate, String endDate) {
        return domainService.anomalies(startDate, endDate);
    }

    public List<AnalyticsResponses.ForecastQuality> forecastQuality(String date) {
        return domainService.forecastQuality(date);
    }
}
