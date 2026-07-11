package com.lorafilm.movie.common.api;

public record InvalidDateFormatData(String field, Object rejectedValue, String expectedFormat) {
}
