package com.project.userservice.dto.response;

public record PiiGovernanceSummaryResponse(
        long totalProfiles,
        long protectedProfiles,
        long dueForErasure,
        long erasedProfiles,
        int activeKeyVersion
) {
}
