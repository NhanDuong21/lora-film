package com.lorafilm.movie.autoschedule.model;

public class CandidateValidationResult {
    private final boolean valid;
    private final String rejectionCode;
    private final String rejectionReason;

    public CandidateValidationResult(boolean valid, String rejectionCode, String rejectionReason) {
        this.valid = valid;
        this.rejectionCode = rejectionCode;
        this.rejectionReason = rejectionReason;
    }

    public static CandidateValidationResult valid() {
        return new CandidateValidationResult(true, null, null);
    }

    public static CandidateValidationResult rejected(String code, String reason) {
        return new CandidateValidationResult(false, code, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getRejectionCode() {
        return rejectionCode;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
