package com.lorafilm.movie.common.api;

import java.util.List;

public record InvalidEnumErrorData(String field, Object rejectedValue, List<String> allowedValues) {
}
