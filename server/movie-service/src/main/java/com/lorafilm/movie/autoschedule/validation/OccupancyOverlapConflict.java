package com.lorafilm.movie.autoschedule.validation;

/**
 * Internal conflict details. Endpoint adapters decide which public error code to expose.
 */
public record OccupancyOverlapConflict(
        Long auditoriumId,
        String priorItemPublicId,
        String currentItemPublicId) {
}
