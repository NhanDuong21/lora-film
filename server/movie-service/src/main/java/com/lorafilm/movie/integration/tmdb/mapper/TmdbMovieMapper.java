package com.lorafilm.movie.integration.tmdb.mapper;

import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieDto;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TmdbMovieMapper {

    @Mapping(target = "id", ignore = true) // Database auto-generated ID
    @Mapping(target = "tmdbId", source = "id") // Map TMDB's ID to our tmdbId
    @Mapping(target = "publicId", expression = "java(generatePublicId())")
    @Mapping(target = "durationMinutes", source = "runtime")
    @Mapping(target = "synopsis", source = "overview")
    @Mapping(target = "status", expression = "java(getDefaultStatus())")
    @Mapping(target = "releaseDate", dateFormat = "yyyy-MM-dd")
    @Mapping(target = "slug", expression = "java(generateSlug(dto.getTitle()))")
    Movie toEntity(TmdbMovieDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tmdbId", ignore = true)
    @Mapping(target = "publicId", ignore = true)
    @Mapping(target = "durationMinutes", source = "runtime")
    @Mapping(target = "synopsis", source = "overview")
    @Mapping(target = "releaseDate", dateFormat = "yyyy-MM-dd")
    void updateEntityFromDto(TmdbMovieDto dto, @MappingTarget Movie entity);

    default String generatePublicId() {
        return UUID.randomUUID().toString();
    }

    default MovieStatus getDefaultStatus() {
        return MovieStatus.DRAFT;
    }

    default String generateSlug(String title) {
        if (title == null) return UUID.randomUUID().toString();
        return title.toLowerCase().replaceAll("[^a-z0-9\\\\s-]", "").replaceAll("\\\\s+", "-");
    }
}
