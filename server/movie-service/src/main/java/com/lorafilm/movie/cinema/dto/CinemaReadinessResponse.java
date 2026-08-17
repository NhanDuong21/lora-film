package com.lorafilm.movie.cinema.dto;

import java.util.List;

public record CinemaReadinessResponse(
        String cinemaPublicId,
        boolean readyForActivation,
        int completedOperationalChecks,
        int totalOperationalChecks,
        int readyAuditoriums,
        int totalAuditoriums,
        List<ReadinessCheck> operationalChecks,
        List<ReadinessCheck> publicProfileChecks) {

    public record ReadinessCheck(
            String id,
            String label,
            boolean complete,
            String reason,
            String actionTab) {
    }
}
