package com.lorafilm.movie.common.api;

import java.util.List;

public record ValidationErrorData(List<FieldErrorDetail> fieldErrors) {
}
