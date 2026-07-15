package com.lorafilm.movie.pricing.dto.response;

import java.util.List;

public class ShowtimePricesResponse {

    private String currency;
    private List<ShowtimePriceDto> prices;

    public ShowtimePricesResponse() {}

    public ShowtimePricesResponse(String currency, List<ShowtimePriceDto> prices) {
        this.currency = currency;
        this.prices = prices;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<ShowtimePriceDto> getPrices() {
        return prices;
    }

    public void setPrices(List<ShowtimePriceDto> prices) {
        this.prices = prices;
    }
}
