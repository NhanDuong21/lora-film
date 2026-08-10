package com.lorafilm.movie.auditorium.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ScreenType {
    STANDARD("STANDARD"),
    IMAX("IMAX"),
    FOUR_DX("4DX"),
    SCREENX("SCREENX");

    private final String value;

    ScreenType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ScreenType fromValue(String value) {
        if (value == null) return null;
        for (ScreenType type : ScreenType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + value);
    }
}
