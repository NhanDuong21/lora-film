package com.lorafilm.movie.movie.dto.people;

import java.util.List;

public record PublicPersonCardResponse(
        String id,
        String slug,
        String name,
        String originalName,
        String profileImageUrl,
        List<String> roles,
        List<String> knownFor,
        String characterName,
        long popularityScore) {
}
