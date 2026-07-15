package com.lorafilm.movie.pricing.service;

import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.showtime.domain.entity.Showtime;

public interface ShowtimePricingService {

    ShowtimePricesResponse updatePrices(String showtimePublicId, UpdateShowtimePricesRequest request);

    ShowtimePricesResponse getPrices(String showtimePublicId);

    void validateCompleteness(Showtime showtime);
}
