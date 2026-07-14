package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;

import java.util.List;

public interface CandidateSelectionResolver {
    void resolveDefaultSelection(List<ShowtimeCandidate> candidates);
}
