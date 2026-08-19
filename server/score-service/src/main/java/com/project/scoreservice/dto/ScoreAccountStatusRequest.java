package com.project.scoreservice.dto;

import com.project.scoreservice.enumtype.UserScoreStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ScoreAccountStatusRequest(
        @NotNull UserScoreStatus status,
        @NotBlank String reason,
        String caseId
) {}
