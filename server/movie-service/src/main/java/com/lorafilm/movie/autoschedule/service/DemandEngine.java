package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.DemandCandidateFacts;
import com.lorafilm.movie.autoschedule.model.DemandEstimate;
import com.lorafilm.movie.autoschedule.model.DemandHistorySnapshot;

public interface DemandEngine {
    String modelVersion();

    DemandEstimate estimate(DemandCandidateFacts candidate, DemandHistorySnapshot history);
}
