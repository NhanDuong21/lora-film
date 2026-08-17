package com.lorafilm.movie.cinema.service;

import com.lorafilm.movie.cinema.dto.CinemaClosureImpactResponse;
import com.lorafilm.movie.cinema.dto.CreateCinemaClosurePeriodRequest;

public interface CinemaClosureImpactService {
    CinemaClosureImpactResponse preview(
            String cinemaPublicId,
            CreateCinemaClosurePeriodRequest request);
}
