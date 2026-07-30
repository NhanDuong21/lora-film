package com.lorafilm.movie.pricing.util;

import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;

import java.math.BigDecimal;

public final class SeatPriceAllocation {

    private static final BigDecimal COUPLE_MEMBER_COUNT = BigDecimal.valueOf(2);

    private SeatPriceAllocation() {
    }

    /**
     * ShowtimePrice stores the price of one sellable unit. A COUPLE unit has
     * two physical seat records, so transport DTOs allocate half to each
     * member and preserve the invariant that seat lines sum to the unit price.
     */
    public static BigDecimal perPhysicalSeat(
            SeatTypeCode seatTypeCode, BigDecimal configuredUnitPrice) {
        if (configuredUnitPrice == null || seatTypeCode != SeatTypeCode.COUPLE) {
            return configuredUnitPrice;
        }
        return configuredUnitPrice.divide(COUPLE_MEMBER_COUNT);
    }
}
