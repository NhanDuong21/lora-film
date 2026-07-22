package com.lorafilm.movie.autoschedule.model;

import java.time.Instant;
import java.time.LocalDate;

public record CandidateSlot(LocalDate serviceDate,
                            OperatingWindow operatingWindow,
                            AutoScheduleGenerationContext.AuditoriumSnapshot auditorium,
                            AutoScheduleGenerationContext.MovieVersionSnapshot movieVersion,
                            Instant startTime,
                            Instant endTime,
                            Instant occupancyEndTime) {
}
