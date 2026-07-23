package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface ShowtimeCandidateGenerator {
    long generate(AutoScheduleGenerationContext context, Consumer<ShowtimeCandidate> consumer);

    default List<ShowtimeCandidate> generate(AutoScheduleGenerationContext context) {
        List<ShowtimeCandidate> candidates = new ArrayList<>();
        generate(context, candidates::add);
        return candidates;
    }
}
