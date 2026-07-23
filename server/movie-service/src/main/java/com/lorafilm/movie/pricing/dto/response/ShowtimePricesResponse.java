package com.lorafilm.movie.pricing.dto.response;

import java.util.List;

public class ShowtimePricesResponse {

    private String currency;
    private List<ShowtimePriceDto> prices;
    private boolean complete;
    private List<PriceSeatTypeDiagnosticDto> missingSeatTypes = List.of();
    private List<PriceSeatTypeDiagnosticDto> ambiguousSeatTypes = List.of();

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

    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }
    public List<PriceSeatTypeDiagnosticDto> getMissingSeatTypes() { return missingSeatTypes; }
    public void setMissingSeatTypes(List<PriceSeatTypeDiagnosticDto> missingSeatTypes) { this.missingSeatTypes = missingSeatTypes; }
    public List<PriceSeatTypeDiagnosticDto> getAmbiguousSeatTypes() { return ambiguousSeatTypes; }
    public void setAmbiguousSeatTypes(List<PriceSeatTypeDiagnosticDto> ambiguousSeatTypes) { this.ambiguousSeatTypes = ambiguousSeatTypes; }
}
