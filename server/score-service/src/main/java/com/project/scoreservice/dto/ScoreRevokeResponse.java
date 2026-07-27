package com.project.scoreservice.dto;

public record ScoreRevokeResponse(
    Long userId,
    int requestedPoints,
    int deductedPoints,
    int outstandingPoints,
    int currentPoints,
    int accumulatedPoints,
    String previousTier,
    String currentTier,
    boolean tierChanged,
    String reconciliationStatus,
    boolean requiresManualReconciliation,
    boolean idempotent
) {}
