package com.project.paymentservice.enumtype;

public enum RefundStatus {
    PENDING_APPROVAL,
    REQUESTED,
    PROCESSING,
    SUCCESS,
    FAILED,
    REQUIRES_ACTION,
    CANCELLED,
    REJECTED
}
