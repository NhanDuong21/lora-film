package com.lorafilm.movie.movie.domain.enums;

public enum CreditRoleType {
    DIRECTOR,
    MAIN_ACTOR,
    SUPPORTING_ACTOR,
    VOICE_ACTOR,
    WRITER,
    PRODUCER,
    GUEST;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static CreditRoleType fromString(String value) {
        if (value == null) return null;
        try {
            return CreditRoleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Will trigger @NotNull validation on DTO instead of breaking Jackson parsing completely
        }
    }
}
