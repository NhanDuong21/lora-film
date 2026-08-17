package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.seat.dto.BulkCreateSeatsRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Atomic setup command for the first draft of an auditorium and its booking
 * layout. Either both aggregates are persisted or neither is.
 */
public record CreateAuditoriumWithLayoutRequest(
        @NotNull @Valid CreateAuditoriumRequest auditorium,
        @NotNull @Valid BulkCreateSeatsRequest layout) {
}
