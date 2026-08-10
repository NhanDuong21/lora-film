package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;

import java.util.List;

public class MovieReadinessDto {
    private MovieHealthStatus healthStatus;
    private String classification; // READY, INCOMPLETE, UNKNOWN
    private List<ReadinessIssueDto> blockers;
    private List<ReadinessIssueDto> warnings;

    public MovieReadinessDto() {}

    public MovieReadinessDto(String classification, List<ReadinessIssueDto> blockers, List<ReadinessIssueDto> warnings) {
        this(resolveHealthStatus(blockers, warnings), classification, blockers, warnings);
    }

    public MovieReadinessDto(
            MovieHealthStatus healthStatus,
            String classification,
            List<ReadinessIssueDto> blockers,
            List<ReadinessIssueDto> warnings) {
        this.healthStatus = healthStatus;
        this.classification = classification;
        this.blockers = blockers;
        this.warnings = warnings;
    }

    private static MovieHealthStatus resolveHealthStatus(
            List<ReadinessIssueDto> blockers,
            List<ReadinessIssueDto> warnings) {
        if (blockers != null && !blockers.isEmpty()) {
            return MovieHealthStatus.BLOCKED;
        }
        if (warnings != null && !warnings.isEmpty()) {
            return MovieHealthStatus.WARNING;
        }
        return MovieHealthStatus.READY;
    }

    public MovieHealthStatus getHealthStatus() { return healthStatus; }
    public void setHealthStatus(MovieHealthStatus healthStatus) { this.healthStatus = healthStatus; }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public List<ReadinessIssueDto> getBlockers() { return blockers; }
    public void setBlockers(List<ReadinessIssueDto> blockers) { this.blockers = blockers; }

    public List<ReadinessIssueDto> getWarnings() { return warnings; }
    public void setWarnings(List<ReadinessIssueDto> warnings) { this.warnings = warnings; }
}
