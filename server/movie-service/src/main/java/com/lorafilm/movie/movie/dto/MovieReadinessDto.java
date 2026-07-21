package com.lorafilm.movie.movie.dto;

import java.util.List;

public class MovieReadinessDto {
    private String classification; // READY, INCOMPLETE, UNKNOWN
    private List<ReadinessIssueDto> blockers;
    private List<ReadinessIssueDto> warnings;

    public MovieReadinessDto() {}

    public MovieReadinessDto(String classification, List<ReadinessIssueDto> blockers, List<ReadinessIssueDto> warnings) {
        this.classification = classification;
        this.blockers = blockers;
        this.warnings = warnings;
    }

    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }

    public List<ReadinessIssueDto> getBlockers() { return blockers; }
    public void setBlockers(List<ReadinessIssueDto> blockers) { this.blockers = blockers; }

    public List<ReadinessIssueDto> getWarnings() { return warnings; }
    public void setWarnings(List<ReadinessIssueDto> warnings) { this.warnings = warnings; }
}
