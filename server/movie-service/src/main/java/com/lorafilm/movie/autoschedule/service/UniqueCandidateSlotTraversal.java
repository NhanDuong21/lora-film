package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.CandidateSlot;

import java.util.function.Consumer;

public interface UniqueCandidateSlotTraversal {
    long traverse(AutoScheduleGenerationContext context, long stopAfter, Consumer<CandidateSlot> consumer);
}
