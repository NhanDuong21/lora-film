package com.project.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AnalyticsResponses {
    private AnalyticsResponses() {
    }

    public record Period(LocalDate startDate, LocalDate endDate, String timezone) {
    }

    public record Summary(
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal netRevenue,
            long bookingCount,
            long refundBookingCount,
            long cancelledBookingCount,
            long ticketCount,
            BigDecimal averageBookingValue,
            BigDecimal refundRate,
            BigDecimal occupancyRate,
            BigDecimal promotionUsageRate,
            String currency) {
    }

    public record DailyKpi(
            LocalDate statDate,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal netRevenue,
            long bookingCount,
            long refundBookingCount,
            long cancelledBookingCount,
            long ticketCount,
            long newCustomerCount,
            long returningCustomerCount,
            BigDecimal averageBookingValue,
            BigDecimal refundRate,
            BigDecimal occupancyRate,
            BigDecimal promotionUsageRate,
            BigDecimal dataCompleteness,
            Instant updatedAt) {
    }

    public record CinemaKpi(
            String cinemaKey,
            String cinemaName,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal netRevenue,
            long bookingCount,
            long ticketCount,
            BigDecimal averageBookingValue,
            BigDecimal refundRate,
            BigDecimal occupancyRate) {
    }

    public record MovieKpi(
            String movieKey,
            Long movieId,
            String movieTitle,
            BigDecimal grossRevenue,
            BigDecimal discountAmount,
            BigDecimal refundAmount,
            BigDecimal netRevenue,
            long bookingCount,
            long ticketCount,
            BigDecimal refundRate,
            BigDecimal occupancyRate) {
    }

    public record PromotionKpi(
            String promotionKey,
            String promotionName,
            long usageCount,
            BigDecimal discountCost,
            BigDecimal generatedRevenue,
            BigDecimal roi) {
    }

    public record CustomerSegment(
            LocalDate statDate,
            String membershipTier,
            long activeUsers,
            long newUsers,
            long returningUsers,
            BigDecimal totalSpending,
            BigDecimal averageSpending,
            BigDecimal customerLifetimeValue) {
    }

    public record Forecast(
            String entityType,
            String entityKey,
            LocalDate forecastDate,
            String forecastType,
            LocalDate asOfDate,
            BigDecimal predictedValue,
            BigDecimal predictionLowerBound,
            BigDecimal predictionUpperBound,
            BigDecimal confidenceScore,
            String algorithm,
            String modelVersion,
            LocalDate trainingStartDate,
            LocalDate trainingEndDate,
            Instant generatedAt) {
    }

    public record RootCause(
            int rank,
            String causeType,
            String dimensionType,
            String dimensionKey,
            BigDecimal contributionScore,
            String evidenceJson) {
    }

    public record Insight(
            long id,
            LocalDate statDate,
            String entityType,
            String entityKey,
            String severity,
            String category,
            String title,
            String summary,
            String rootCause,
            String evidenceJson,
            LocalDate baselineStartDate,
            LocalDate baselineEndDate,
            BigDecimal expectedValue,
            BigDecimal actualValue,
            BigDecimal deviationRate,
            String analysisVersion,
            BigDecimal confidenceScore,
            List<RootCause> rootCauses,
            Instant createdAt) {
    }

    public record Recommendation(
            long id,
            long insightId,
            String targetService,
            String actionType,
            String priority,
            String title,
            String description,
            String expectedImpact,
            BigDecimal estimatedImpactValue,
            String impactUnit,
            BigDecimal confidenceScore,
            String status,
            String acceptedBy,
            Instant acceptedAt,
            Instant completedAt,
            Instant expiresAt,
            Instant createdAt) {
    }

    public record Alert(
            long id,
            long insightId,
            String entityType,
            String entityKey,
            String severity,
            String title,
            String message,
            boolean acknowledged,
            String acknowledgedBy,
            Instant acknowledgedAt,
            boolean resolved,
            Instant resolvedAt,
            Instant createdAt) {
    }

    public record HealthScore(
            LocalDate statDate,
            BigDecimal overallScore,
            BigDecimal revenueScore,
            BigDecimal demandScore,
            BigDecimal occupancyScore,
            BigDecimal customerScore,
            BigDecimal operationalScore,
            BigDecimal dataQualityScore,
            String healthStatus,
            BigDecimal confidenceScore,
            String algorithmVersion,
            String driversJson,
            Instant calculatedAt) {
    }

    public record Anomaly(
            long id,
            Long insightId,
            LocalDate statDate,
            String metricName,
            BigDecimal actualValue,
            BigDecimal expectedValue,
            BigDecimal deviationRate,
            BigDecimal anomalyScore,
            String detectionMethod,
            String severity,
            String status,
            String evidenceJson,
            Instant detectedAt) {
    }

    public record ForecastQuality(
            String forecastType,
            String algorithm,
            String modelVersion,
            LocalDate evaluationDate,
            LocalDate testStartDate,
            LocalDate testEndDate,
            int sampleSize,
            BigDecimal mae,
            BigDecimal rmse,
            BigDecimal mape,
            BigDecimal bias,
            Instant calculatedAt) {
    }

    public record ActionResult(long id, String status, Instant updatedAt) {
    }

    public record Job(
            long id,
            String requestId,
            String jobType,
            String mode,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String requestedBy,
            Instant requestedAt,
            Instant startedAt,
            Instant completedAt,
            int processedDays,
            int totalDays,
            String errorMessage) {
    }

    public record DataQuality(
            long paymentFacts,
            long cancellationFacts,
            long refundFacts,
            long processedEvents,
            BigDecimal latestCompleteness,
            String freshnessStatus,
            String lastPipelineStatus,
            LocalDate lastCalculatedDate,
            Instant lastPipelineAt) {
    }

    public record Dashboard(
            Period period,
            Summary summary,
            List<DailyKpi> daily,
            List<CinemaKpi> topCinemas,
            List<MovieKpi> topMovies,
            List<PromotionKpi> promotions,
            List<CustomerSegment> customerSegments,
            List<Forecast> forecasts,
            HealthScore healthScore,
            List<Anomaly> anomalies,
            List<ForecastQuality> forecastQuality,
            List<Insight> insights,
            List<Recommendation> recommendations,
            List<Alert> alerts,
            DataQuality dataQuality) {
    }
}
