package com.lorafilm.movie.pricing.domain.enums;

import java.time.DayOfWeek;

public enum PriceDayType {
    ALL_DAYS,
    WEEKDAY,
    WEEKEND;

    public boolean matches(DayOfWeek dayOfWeek) {
        if (this == ALL_DAYS) {
            return true;
        }
        boolean weekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        return this == WEEKEND ? weekend : !weekend;
    }

    public int rank() {
        return this == ALL_DAYS ? 1 : 2;
    }
}
