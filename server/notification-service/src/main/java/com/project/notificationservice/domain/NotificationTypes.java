package com.project.notificationservice.domain;

public final class NotificationTypes {

    private NotificationTypes() {
    }

    public enum Channel {
        EMAIL,
        IN_APP,
        WEB_PUSH,
        SMS
    }

    public enum Category {
        TRANSACTIONAL,
        SECURITY,
        MARKETING,
        OPERATIONAL
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    public enum RequestStatus {
        ACCEPTED,
        PROCESSING,
        COMPLETED,
        PARTIALLY_FAILED,
        FAILED,
        CANCELLED
    }

    public enum DeliveryStatus {
        PENDING,
        PROCESSING,
        RETRY_SCHEDULED,
        SENT,
        DELIVERED,
        FAILED,
        DEAD_LETTERED,
        CANCELLED,
        SUPPRESSED
    }

    public enum FailureCategory {
        TRANSIENT,
        PERMANENT,
        RATE_LIMITED,
        INVALID_RECIPIENT,
        PROVIDER_REJECTED,
        TEMPLATE_ERROR,
        PAYLOAD_ERROR,
        AUTHENTICATION_ERROR
    }

    public enum TemplateStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}
