package com.project.promotionservice.automation.enums;

public enum AudienceMemberStatus {
    PENDING, ISSUED, SKIPPED_ALREADY_GRANTED, SKIPPED_INELIGIBLE,
    FAILED_RETRYABLE, FAILED_FINAL, REVOCATION_PENDING, ANOMALY_REVIEW_REQUIRED
}
