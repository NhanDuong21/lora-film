package com.lorafilm.movie.auditorium.dto;

import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;

import java.util.List;

public record AuditoriumLayoutSourcePreview(
        String sourceType,
        String sourcePublicId,
        String name,
        String scope,
        Integer layoutVersion,
        String description,
        ScreenType recommendedScreenType,
        SoundType recommendedSoundType,
        Integer rows,
        Integer columns,
        Integer capacity,
        Integer ticketingPositions,
        Integer standardSeats,
        Integer vipSeats,
        Integer coupleModules,
        Integer coupleSeats,
        Integer accessiblePositions,
        Integer aisleCount,
        Integer doorCount,
        List<List<String>> matrix,
        boolean valid,
        List<ValidationItem> validation
) {
    public record ValidationItem(String code, String label, boolean passed, String severity) {}
}
