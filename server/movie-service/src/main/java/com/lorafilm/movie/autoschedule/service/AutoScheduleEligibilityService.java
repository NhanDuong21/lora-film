package com.lorafilm.movie.autoschedule.service;

import java.time.LocalDate;
import java.util.List;
import com.lorafilm.movie.autoschedule.dto.response.EligibleMovieResponse;

public interface AutoScheduleEligibilityService {
    List<EligibleMovieResponse> getEligibleMovies(LocalDate fromDate, LocalDate toDate);
}
