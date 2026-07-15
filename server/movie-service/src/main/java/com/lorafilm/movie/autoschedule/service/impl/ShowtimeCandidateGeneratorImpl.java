package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.model.CandidateGenerationContext;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ShowtimeCandidateGeneratorImpl implements ShowtimeCandidateGenerator {

    private static final int MAX_GENERATED_CANDIDATES = 10_000;

    private final CinemaOperatingWindowResolver windowResolver;

    public ShowtimeCandidateGeneratorImpl(CinemaOperatingWindowResolver windowResolver) {
        this.windowResolver = windowResolver;
    }

    @Override
    public List<ShowtimeCandidate> generate(CandidateGenerationContext context) {
        List<OperatingWindow> windows = windowResolver.resolve(context.getCinema(),
                context.getRequest().getScheduleFrom(), context.getRequest().getScheduleTo());

        List<ShowtimeCandidate> candidates = new ArrayList<>();

        for (OperatingWindow window : windows) {
            for (Auditorium auditorium : context.getAuditoriums()) {
                for (MovieVersion movieVersion : context.getMovieVersions()) {
                    int durationMinutes = movieVersion.getMovie().getDurationMinutes() != null ? movieVersion.getMovie().getDurationMinutes() : 0;
                    if (durationMinutes <= 0) {
                        continue; // Invalid duration, handled in validation later or skip here.
                    }

                    int slotGranularity = context.getRequest().getSlotGranularityMinutes();
                    Instant slotStart = window.getOpenInstant();

                    while (!slotStart.isAfter(window.getCloseInstant())) {
                        Instant slotEnd = slotStart.plus(durationMinutes, ChronoUnit.MINUTES);
                        int bufferMinutes = auditorium.getCleaningBufferMinutes() != null ? auditorium.getCleaningBufferMinutes() : 0;
                        Instant occupancyEnd = slotEnd.plus(bufferMinutes, ChronoUnit.MINUTES);

                        // Basic check: Candidate start time must be within the window. 
                        // End time can technically stretch beyond, but it will be validated by the business rules if it exceeds operating hours.
                        
                        ShowtimeCandidate candidate = new ShowtimeCandidate();
                        candidate.setCinema(context.getCinema());
                        candidate.setAuditorium(auditorium);
                        candidate.setMovieVersion(movieVersion);
                        candidate.setMovie(movieVersion.getMovie());
                        candidate.setStartTime(slotStart);
                        candidate.setEndTime(slotEnd);
                        candidate.setOccupancyEndTime(occupancyEnd);

                        candidates.add(candidate);

                        if (candidates.size() > MAX_GENERATED_CANDIDATES) {
                            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_TOO_MANY_CANDIDATES);
                        }

                        slotStart = slotStart.plus(slotGranularity, ChronoUnit.MINUTES);
                    }
                }
            }
        }

        return candidates;
    }
}
