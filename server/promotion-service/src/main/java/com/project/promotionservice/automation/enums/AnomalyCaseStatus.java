package com.project.promotionservice.automation.enums;

public enum AnomalyCaseStatus {
    OPEN,
    IN_REVIEW,
    RESOLVED_ACCEPTED_COST,
    RESOLVED_CUSTOMER_ABUSE,
    DISMISSED_TEST_DATA;

    public boolean isOpen() {
        return this == OPEN || this == IN_REVIEW;
    }
}
