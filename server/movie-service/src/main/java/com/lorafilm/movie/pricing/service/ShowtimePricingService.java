package com.lorafilm.movie.pricing.service;

import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.showtime.domain.entity.Showtime;

public interface ShowtimePricingService {

    ShowtimePricesResponse updatePrices(String showtimePublicId, UpdateShowtimePricesRequest request);

    ShowtimePricesResponse getPrices(String showtimePublicId);

    ShowtimePricesResponse resolvePrices(String showtimePublicId, Long expectedShowtimeVersion);

    com.lorafilm.movie.pricing.service.model.PriceResolutionResult resolveAndReplace(Showtime showtime);

    java.util.List<com.lorafilm.movie.pricing.service.model.PriceResolutionResult> resolveAndReplaceAll(
            java.util.List<Showtime> showtimes);

    void replaceAllWithResolvedResults(
            java.util.List<Showtime> showtimes,
            java.util.List<com.lorafilm.movie.pricing.service.model.PriceResolutionResult> results);

    void validateCompleteness(Showtime showtime);
}
