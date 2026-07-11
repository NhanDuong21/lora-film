package com.lorafilm.movie.common.api;

public record FieldErrorDetail(String field, Object rejectedValue, String message) {
}
