package com.lorafilm.movie.auditorium.dto;

import jakarta.validation.constraints.NotBlank;

public record CloneAuditoriumRequest(
        @NotBlank String sourceAuditoriumPublicId
) {}
