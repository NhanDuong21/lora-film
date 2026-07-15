package com.lorafilm.movie.pricing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UpdateShowtimePricesRequest {

    @NotEmpty(message = "Prices list cannot be empty")
    @Valid
    private List<ShowtimePriceItemRequest> prices;

    public List<ShowtimePriceItemRequest> getPrices() {
        return prices;
    }

    public void setPrices(List<ShowtimePriceItemRequest> prices) {
        this.prices = prices;
    }
}
