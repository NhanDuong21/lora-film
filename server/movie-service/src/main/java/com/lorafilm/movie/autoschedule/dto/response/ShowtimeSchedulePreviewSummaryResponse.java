package com.lorafilm.movie.autoschedule.dto.response;

public class ShowtimeSchedulePreviewSummaryResponse {

    private Integer totalCandidateCount;
    private Integer validCandidateCount;
    private Integer rejectedCandidateCount;
    private Integer selectedCandidateCount;

    public ShowtimeSchedulePreviewSummaryResponse() {
    }

    public Integer getTotalCandidateCount() {
        return totalCandidateCount;
    }

    public void setTotalCandidateCount(Integer totalCandidateCount) {
        this.totalCandidateCount = totalCandidateCount;
    }

    public Integer getValidCandidateCount() {
        return validCandidateCount;
    }

    public void setValidCandidateCount(Integer validCandidateCount) {
        this.validCandidateCount = validCandidateCount;
    }

    public Integer getRejectedCandidateCount() {
        return rejectedCandidateCount;
    }

    public void setRejectedCandidateCount(Integer rejectedCandidateCount) {
        this.rejectedCandidateCount = rejectedCandidateCount;
    }

    public Integer getSelectedCandidateCount() {
        return selectedCandidateCount;
    }

    public void setSelectedCandidateCount(Integer selectedCandidateCount) {
        this.selectedCandidateCount = selectedCandidateCount;
    }
}
