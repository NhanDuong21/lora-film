package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.model.DemandHistorySnapshot;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public interface DemandHistoryProvider {
    DemandHistorySnapshot load(String cinemaPublicId,
                               ZoneId cinemaZone,
                               LocalDate historyFrom,
                               LocalDate historyTo,
                               List<String> moviePublicIds);
}
