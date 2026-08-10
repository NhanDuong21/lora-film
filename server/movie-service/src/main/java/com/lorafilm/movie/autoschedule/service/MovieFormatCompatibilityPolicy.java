package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import org.springframework.stereotype.Component;

@Component
public class MovieFormatCompatibilityPolicy {
    public boolean isCompatible(MovieFormat format, ScreenType screenType) {
        if (format == null || screenType == null) return false;
        return switch (format) {
            case IMAX -> screenType == ScreenType.IMAX;
            case FOUR_DX -> screenType == ScreenType.FOUR_DX;
            case SCREENX -> screenType == ScreenType.SCREENX;
            case TWO_D, THREE_D -> true;
        };
    }
}
