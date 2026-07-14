package com.lorafilm.movie.movie.domain.enums;

public enum CompanyRoleType {
    PRODUCTION,
    DISTRIBUTOR,
    STUDIO;

    @com.fasterxml.jackson.annotation.JsonCreator
    public static CompanyRoleType fromString(String value) {
        if (value == null) return null;
        try {
            return CompanyRoleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // Will trigger @NotNull validation on DTO instead of breaking Jackson parsing completely
        }
    }
}
