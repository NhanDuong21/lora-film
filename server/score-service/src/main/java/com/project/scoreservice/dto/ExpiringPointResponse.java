package com.project.scoreservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpiringPointResponse(
    Long id,
    int earnedPoints,
    int remainingPoints,
    LocalDate expirationDate,
    String status,
    Long bookingId,
    LocalDateTime createdAt
) {}
