package com.project.promotionservice.integration.inbox;

public enum IntegrationEventStatus {
    RECEIVED,
    PROCESSING,
    COMPLETED,
    RETRY,
    DEAD_LETTER,
    IGNORED
}
