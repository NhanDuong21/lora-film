package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateGenerator;
import com.lorafilm.movie.autoschedule.service.UniqueCandidateSlotTraversal;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class ShowtimeCandidateGeneratorImpl implements ShowtimeCandidateGenerator {

    private final UniqueCandidateSlotTraversal traversal;

    public ShowtimeCandidateGeneratorImpl(UniqueCandidateSlotTraversal traversal) {
        this.traversal = traversal;
    }

    @Override
    public long generate(AutoScheduleGenerationContext context, Consumer<ShowtimeCandidate> consumer) {
        return traversal.traverse(context, Long.MAX_VALUE, slot -> {
            ShowtimeCandidate candidate = new ShowtimeCandidate();
            candidate.setCinemaSnapshot(context.getCinema());
            candidate.setAuditoriumSnapshot(slot.auditorium());
            candidate.setMovieVersionSnapshot(slot.movieVersion());
            candidate.setOperatingWindow(slot.operatingWindow());
            candidate.setStartTime(slot.startTime());
            candidate.setEndTime(slot.endTime());
            candidate.setOccupancyEndTime(slot.occupancyEndTime());
            consumer.accept(candidate);
        });
    }
}
