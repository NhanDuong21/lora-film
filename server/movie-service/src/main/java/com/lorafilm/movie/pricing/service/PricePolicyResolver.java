package com.lorafilm.movie.pricing.service;

import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.showtime.domain.entity.Showtime;

import java.util.List;

public interface PricePolicyResolver {
    PriceResolutionResult resolve(Showtime showtime);

    List<PriceResolutionResult> resolveAll(List<Showtime> showtimes);
}
