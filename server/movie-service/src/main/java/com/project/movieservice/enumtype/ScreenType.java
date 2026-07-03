package com.project.movieservice.enumtype;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ScreenType {
    STANDARD,
    IMAX,
    @JsonProperty("4DX")
    _4DX;
    
    // Custom deserializer to handle "4DX" string from request
    public static ScreenType fromString(String value) {
        if ("4DX".equalsIgnoreCase(value)) {
            return _4DX;
        }
        for (ScreenType type : ScreenType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + value);
    }
}
