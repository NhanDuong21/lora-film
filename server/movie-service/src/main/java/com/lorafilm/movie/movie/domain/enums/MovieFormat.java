package com.lorafilm.movie.movie.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MovieFormat {
    TWO_D("2D"),
    THREE_D("3D"),
    IMAX("IMAX"),
    FOUR_DX("4DX"),
    SCREENX("SCREENX");

    private final String value;

    MovieFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MovieFormat fromValue(String value) {
        if (value == null) return null;
        for (MovieFormat format : MovieFormat.values()) {
            if (format.value.equalsIgnoreCase(value) || format.name().equalsIgnoreCase(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + value);
    }
}
