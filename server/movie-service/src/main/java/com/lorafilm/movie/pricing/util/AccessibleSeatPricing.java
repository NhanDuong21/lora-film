package com.lorafilm.movie.pricing.util;

import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;

import java.util.List;

public final class AccessibleSeatPricing {

    private AccessibleSeatPricing() {
    }

    public static ShowtimePrice findPrice(List<ShowtimePrice> prices, SeatType seatType) {
        ShowtimePrice exact = prices.stream()
                .filter(price -> price.getSeatType().getId().equals(seatType.getId()))
                .findFirst()
                .orElse(null);
        if (exact != null || seatType.getCode() != SeatTypeCode.DISABLED) {
            return exact;
        }
        return prices.stream()
                .filter(price -> price.getSeatType().getCode() == SeatTypeCode.STANDARD)
                .findFirst()
                .orElse(null);
    }
}
