package com.lorafilm.movie.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ActiveStatus {
    ACTIVE,
    INACTIVE;

    @JsonCreator
    public static ActiveStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (ActiveStatus status : ActiveStatus.values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid status: " + value);
    }
}
